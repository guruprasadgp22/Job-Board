package com.guru.jobboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.dto.CleanupResult;
import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.entity.Job;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.repository.RemovedJobRepository;
import com.guru.jobboard.source.JobSource;

/**
 * Deletes jobs (and the application tracked on them) once the posting stops accepting applications:
 * <ol>
 *   <li>its application deadline (from JSearch or the page's JobPosting data) has passed (any status),</li>
 *   <li>the job page returns 404/410 or says "no longer accepting applications" (any status),</li>
 *   <li>a complete-listing source stopped listing it for {@code unlisted-days} (untouched jobs only; jobs you
 *       applied to get a link check first),</li>
 *   <li>it is older than {@code max-age-days} and you never acted on it.</li>
 * </ol>
 */
@Service
public class JobCleanupService {

    private static final Logger log = LoggerFactory.getLogger(JobCleanupService.class);
    private static final Set<ApplicationStatus> UNTOUCHED =
            EnumSet.of(ApplicationStatus.NEW, ApplicationStatus.SAVED, ApplicationStatus.REJECTED);

    private final JobRepository jobs;
    private final RemovedJobRepository removedJobs;
    private final JobRemover remover;
    private final LinkChecker linkChecker;
    private final JobSyncService syncService;
    private final List<JobSource> sources;
    private final AppProperties.Cleanup config;
    private final ReentrantLock lock = new ReentrantLock();

    public JobCleanupService(JobRepository jobs, RemovedJobRepository removedJobs, JobRemover remover,
                             LinkChecker linkChecker, JobSyncService syncService, List<JobSource> sources,
                             AppProperties props) {
        this.jobs = jobs;
        this.removedJobs = removedJobs;
        this.remover = remover;
        this.linkChecker = linkChecker;
        this.syncService = syncService;
        this.sources = sources;
        this.config = props.cleanup();
    }

    @Scheduled(cron = "${app.cleanup.cron}")
    public void scheduledCleanup() {
        cleanup();
    }

    public CleanupResult cleanup() {
        if (!lock.tryLock()) {
            return new CleanupResult(0, 0, 0, 0, 0, List.of("cleanup already running"));
        }
        try {
            List<String> removed = new ArrayList<>();
            int expired = removeExpired(removed);
            int byAge = removeOld(removed);
            int unlisted = removeUnlisted(removed);
            int[] linkResult = checkLinks(removed);
            removedJobs.deleteOlderThan(Instant.now().minus(Duration.ofDays(config.maxAgeDays() * 2L)));
            log.info("Cleanup done: {} past deadline, {} closed by link check ({} checked), {} unlisted, {} too old",
                    expired, linkResult[1], linkResult[0], unlisted, byAge);
            return new CleanupResult(linkResult[0], linkResult[1], unlisted, byAge, expired, removed);
        } finally {
            lock.unlock();
        }
    }

    /** Checks a single job now; deletes it if the posting is closed. */
    public LinkChecker.Result checkOne(Job job) {
        LinkChecker.Result result = linkChecker.check(job.getUrl());
        if (result == LinkChecker.Result.CLOSED) {
            log.info("Removing closed job #{} {} ({})", job.getId(), job.getTitle(), job.getUrl());
            remover.remove(job, "posting closed");
        } else {
            job.setLastCheckedAt(Instant.now());
            jobs.save(job);
        }
        return result;
    }

    private int removeExpired(List<String> removed) {
        List<Job> expired = jobs.findByExpiresAtBefore(Instant.now());
        for (Job j : expired) {
            removed.add(label(j, "application deadline passed"));
            remover.remove(j, "deadline passed");
        }
        return expired.size();
    }

    private int removeOld(List<String> removed) {
        Instant cutoff = Instant.now().minus(Duration.ofDays(config.maxAgeDays()));
        List<Job> old = jobs.findByStatusIn(UNTOUCHED).stream()
                .filter(j -> !j.isAddedManually())
                .filter(j -> (j.getPostedAt() != null ? j.getPostedAt() : j.getFirstSeenAt()).isBefore(cutoff))
                .toList();
        for (Job j : old) {
            removed.add(label(j, "older than " + config.maxAgeDays() + " days"));
            remover.remove(j, "too old");
        }
        return old.size();
    }

    private int removeUnlisted(List<String> removed) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(Duration.ofDays(config.unlistedDays()));
        Map<String, Instant> lastSuccess = syncService.lastSuccess();
        int count = 0;
        for (JobSource source : sources) {
            Instant success = lastSuccess.get(source.name());
            // Only trust "missing from results" when the source lists everything and answered recently.
            if (!source.completeListing() || success == null || success.isBefore(now.minus(Duration.ofDays(1)))) {
                continue;
            }
            for (Job job : jobs.findBySourceAndStatusInAndLastSeenAtBefore(
                    source.name(), EnumSet.allOf(ApplicationStatus.class), cutoff)) {
                if (job.isAddedManually()) {
                    continue;
                }
                if (UNTOUCHED.contains(job.getStatus())) {
                    removed.add(label(job, "no longer listed by " + source.name()));
                    remover.remove(job, "no longer listed");
                    count++;
                } else if (checkOne(job) == LinkChecker.Result.CLOSED) {
                    removed.add(label(job, "posting closed"));
                    count++;
                }
            }
        }
        return count;
    }

    /** Returns {checked, closed}. */
    private int[] checkLinks(List<String> removed) {
        Instant recent = Instant.now().minus(Duration.ofHours(12));
        int checked = 0;
        int closed = 0;
        for (Job job : jobs.findLeastRecentlyChecked(PageRequest.of(0, config.linkChecksPerRun()))) {
            if (job.getLastCheckedAt() != null && job.getLastCheckedAt().isAfter(recent)) {
                break;
            }
            checked++;
            if (checkOne(job) == LinkChecker.Result.CLOSED) {
                removed.add(label(job, "posting closed"));
                closed++;
            }
        }
        return new int[] {checked, closed};
    }

    private static String label(Job job, String reason) {
        return job.getTitle() + (job.getCompany() == null ? "" : " @ " + job.getCompany()) + " — " + reason;
    }
}
