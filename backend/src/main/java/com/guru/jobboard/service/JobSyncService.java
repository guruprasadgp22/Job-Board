package com.guru.jobboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.dto.SyncResult;
import com.guru.jobboard.entity.Job;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.repository.PreferenceRepository;
import com.guru.jobboard.repository.RemovedJobRepository;
import com.guru.jobboard.source.FetchedJob;
import com.guru.jobboard.source.JobSource;

/** Pulls jobs from every enabled source and stores, for each user, the ones matching their resume/preferences. */
@Service
public class JobSyncService {

    private static final Logger log = LoggerFactory.getLogger(JobSyncService.class);

    private final List<JobSource> sources;
    private final JobRepository jobs;
    private final RemovedJobRepository removedJobs;
    private final PreferenceRepository preferenceRepository;
    private final PreferenceService preferences;
    private final RelevanceScorer scorer;
    private final TransactionTemplate tx;
    private final int maxAgeDays;
    private final ReentrantLock lock = new ReentrantLock();

    /** When each source last returned successfully; cleanup only treats jobs as unlisted for these sources. */
    private final Map<String, Instant> lastSuccess = new ConcurrentHashMap<>();

    public JobSyncService(List<JobSource> sources, JobRepository jobs, RemovedJobRepository removedJobs,
                          PreferenceRepository preferenceRepository, PreferenceService preferences,
                          RelevanceScorer scorer, TransactionTemplate tx, AppProperties props) {
        this.sources = sources;
        this.jobs = jobs;
        this.removedJobs = removedJobs;
        this.preferenceRepository = preferenceRepository;
        this.preferences = preferences;
        this.scorer = scorer;
        this.tx = tx;
        this.maxAgeDays = props.cleanup().maxAgeDays();
    }

    @Scheduled(cron = "${app.sync.cron}")
    public void scheduledSync() {
        syncAll();
    }

    /** "Find jobs now" for one user. */
    public SyncResult syncUser(long userId) {
        return run(List.of(preferences.get(userId))).get(userId);
    }

    /** Every user; used by the schedule and the external cron. */
    public Map<Long, SyncResult> syncAll() {
        return run(preferenceRepository.findAll());
    }

    public Map<String, Instant> lastSuccess() {
        return Map.copyOf(lastSuccess);
    }

    private Map<Long, SyncResult> run(List<Preference> prefs) {
        Map<Long, SyncResult> results = new LinkedHashMap<>();
        if (!lock.tryLock()) {
            prefs.forEach(p -> results.put(p.getUserId(),
                    new SyncResult(0, 0, Map.of("sync", "already running, try again in a minute"))));
            return results;
        }
        try {
            Map<String, List<FetchedJob>> cache = new HashMap<>();
            Map<String, String> failed = new HashMap<>();
            for (Preference pref : prefs) {
                results.put(pref.getUserId(), syncOne(pref, cache, failed));
            }
            return results;
        } finally {
            lock.unlock();
        }
    }

    private SyncResult syncOne(Preference pref, Map<String, List<FetchedJob>> cache, Map<String, String> failed) {
        Map<String, String> perSource = new LinkedHashMap<>();
        if (isBlank(pref.getKeywords()) && isBlank(pref.getResumeSkills()) && isBlank(pref.getDesiredRole())) {
            perSource.put("sync", "Add your desired role or upload your resume in your profile first");
            return new SyncResult(0, 0, perSource);
        }
        int added = 0;
        int updated = 0;
        for (JobSource source : sources) {
            if (!source.enabled()) {
                perSource.put(source.name(), "disabled (no API key)");
                continue;
            }
            String key = source.name() + "|" + source.cacheKey(pref);
            if (failed.containsKey(key)) {
                perSource.put(source.name(), failed.get(key));
                continue;
            }
            List<FetchedJob> fetched = cache.get(key);
            if (fetched == null) {
                Instant started = Instant.now();
                try {
                    fetched = source.fetch(pref);
                    cache.put(key, fetched);
                    lastSuccess.put(source.name(), started);
                } catch (Exception e) {
                    log.warn("Source {} failed: {}", source.name(), e.toString());
                    String message = "failed: " + e.getClass().getSimpleName();
                    failed.put(key, message);
                    perSource.put(source.name(), message);
                    continue;
                }
            }
            int[] counts = save(fetched, pref);
            added += counts[0];
            updated += counts[1];
            perSource.put(source.name(), "checked " + fetched.size() + ", new " + counts[0] + ", matched " + (counts[0] + counts[1]));
        }
        log.info("Sync for user {}: {} new, {} updated, {}", pref.getUserId(), added, updated, perSource);
        pref.setLastSyncAt(Instant.now());
        preferences.save(pref);
        return new SyncResult(added, updated, perSource);
    }

    /** Returns {added, updated}. */
    private int[] save(List<FetchedJob> fetched, Preference pref) {
        int[] counts = new int[2];
        Map<String, FetchedJob> unique = new LinkedHashMap<>();
        for (FetchedJob f : fetched) {
            if (f.externalId() != null && !f.externalId().isBlank() && f.title() != null && f.url() != null) {
                unique.putIfAbsent(f.externalId(), f);
            }
        }
        Long userId = pref.getUserId();
        tx.executeWithoutResult(status -> {
            Instant now = Instant.now();
            Instant tooOld = now.minus(Duration.ofDays(maxAgeDays));
            for (FetchedJob f : unique.values()) {
                var existing = jobs.findByUserIdAndSourceAndExternalId(userId, f.source(), f.externalId());
                if (existing.isPresent()) {
                    // Still listed by the source: keep it, even if your preferences changed since.
                    Job job = existing.get();
                    job.setLastSeenAt(now);
                    if (f.expiresAt() != null) {
                        job.setExpiresAt(f.expiresAt());
                    }
                    counts[1]++;
                    continue;
                }
                if (f.postedAt() != null && f.postedAt().isBefore(tooOld)) {
                    continue;
                }
                RelevanceScorer.Match match = scorer.score(f, pref);
                if (match.score() <= 0 || match.score() < pref.getMinScore()
                        || removedJobs.existsByUserIdAndSourceAndExternalId(userId, f.source(), f.externalId())) {
                    continue;
                }
                jobs.save(toEntity(userId, f, match, now));
                counts[0]++;
            }
        });
        return counts;
    }

    /** Re-scores a user's stored jobs after their resume or preferences change (no fetching). */
    public void rescore(long userId) {
        Preference pref = preferences.get(userId);
        tx.executeWithoutResult(status -> {
            for (Job job : jobs.findByUserId(userId)) {
                RelevanceScorer.Match m = scorer.score(job.getTitle(), job.getTags(), job.getDescription(),
                        job.getLocation(), job.isRemote(), pref);
                // Jobs you pasted yourself keep their 100% score; they only get the skill breakdown.
                if (!job.isAddedManually()) {
                    job.setRelevanceScore(Math.max(0, m.score()));
                }
                applySkills(job, m);
            }
        });
    }

    private static Job toEntity(Long userId, FetchedJob f, RelevanceScorer.Match match, Instant now) {
        Job job = new Job();
        job.setUserId(userId);
        job.setSource(f.source());
        job.setExternalId(truncate(f.externalId(), 300));
        job.setTitle(truncate(f.title(), 500));
        job.setCompany(truncate(f.company(), 300));
        job.setLocation(truncate(f.location(), 300));
        job.setRemote(f.remote());
        job.setUrl(truncate(f.url(), 2000));
        job.setDescription(f.description());
        job.setTags(truncate(f.tags() == null ? null : String.join(", ", f.tags()), 1000));
        job.setSalary(truncate(f.salary(), 200));
        job.setPostedAt(f.postedAt());
        job.setExpiresAt(f.expiresAt());
        job.setFirstSeenAt(now);
        job.setLastSeenAt(now);
        job.setRelevanceScore(match.score());
        applySkills(job, match);
        return job;
    }

    static void applySkills(Job job, RelevanceScorer.Match match) {
        job.setMatchedSkills(truncate(String.join(", ", match.matched()), 1000));
        job.setMissingSkills(truncate(String.join(", ", match.missing()), 1000));
    }

    static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
