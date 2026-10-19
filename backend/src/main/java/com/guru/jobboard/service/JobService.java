package com.guru.jobboard.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.dto.JobDto;
import com.guru.jobboard.dto.JobUpdateRequest;
import com.guru.jobboard.dto.ManualJobRequest;
import com.guru.jobboard.dto.PageDto;
import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.entity.Job;
import com.guru.jobboard.repository.JobRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class JobService {

    private final JobRepository jobs;
    private final JobRemover remover;
    private final PreferenceService preferences;
    private final RelevanceScorer scorer;

    public JobService(JobRepository jobs, JobRemover remover, PreferenceService preferences, RelevanceScorer scorer) {
        this.jobs = jobs;
        this.remover = remover;
        this.preferences = preferences;
        this.scorer = scorer;
    }

    @Transactional(readOnly = true)
    public PageDto<JobDto> search(long userId, String q, List<ApplicationStatus> statuses, String source, Boolean remote,
                                  int minScore, String sort, int page, int size) {
        Specification<Job> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("userId"), userId));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("company")), like),
                        cb.like(cb.lower(root.get("location")), like),
                        cb.like(cb.lower(root.get("tags")), like)));
            }
            if (statuses != null && !statuses.isEmpty()) {
                p.add(root.get("status").in(statuses));
            }
            if (source != null && !source.isBlank()) {
                p.add(cb.equal(root.get("source"), source));
            }
            if (remote != null) {
                p.add(cb.equal(root.get("remote"), remote));
            }
            if (minScore > 0) {
                p.add(cb.greaterThanOrEqualTo(root.get("relevanceScore"), minScore));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        Sort order = switch (sort == null ? "" : sort) {
            case "newest" -> Sort.by(Sort.Order.desc("firstSeenAt"), Sort.Order.desc("id"));
            case "company" -> Sort.by("company", "title");
            default -> Sort.by(Sort.Order.desc("relevanceScore"), Sort.Order.desc("firstSeenAt"));
        };
        Page<Job> result = jobs.findAll(spec, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), order));
        return new PageDto<>(result.map(j -> JobDto.from(j, false)).getContent(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public JobDto get(long userId, long id) {
        return JobDto.from(find(userId, id), true);
    }

    @Transactional
    public JobDto addManual(long userId, ManualJobRequest req) {
        String url = req.url().trim();
        String source = JobSites.fromUrl(url);
        jobs.findByUserIdAndSourceAndExternalId(userId, source, JobSyncService.truncate(url, 300)).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already added this job (#" + existing.getId() + ")");
        });
        Instant now = Instant.now();
        Job job = new Job();
        job.setUserId(userId);
        job.setSource(source);
        job.setAddedManually(true);
        job.setExpiresAt(req.expiresAt());
        job.setExternalId(JobSyncService.truncate(url, 300));
        job.setUrl(url);
        job.setTitle(req.title().trim());
        job.setCompany(req.company());
        job.setLocation(req.location());
        job.setRemote(req.remote());
        job.setTags(JobSyncService.truncate(req.tags(), 1000));
        job.setSalary(req.salary());
        job.setDescription(req.description());
        job.setNotes(req.notes());
        job.setPostedAt(now);
        job.setFirstSeenAt(now);
        job.setLastSeenAt(now);
        job.setRelevanceScore(100);
        job.setStatus(ApplicationStatus.SAVED);
        JobSyncService.applySkills(job, scorer.score(job.getTitle(), job.getTags(), job.getDescription(), job.getLocation(),
                job.isRemote(), preferences.get(userId)));
        return JobDto.from(jobs.save(job), true);
    }

    @Transactional
    public JobDto update(long userId, long id, JobUpdateRequest req) {
        Job job = find(userId, id);
        if (req.status() != null && req.status() != job.getStatus()) {
            if (req.status().isActive() && job.getAppliedAt() == null) {
                job.setAppliedAt(Instant.now());
            }
            job.setStatus(req.status());
        }
        if (req.notes() != null) {
            job.setNotes(req.notes());
        }
        return JobDto.from(job, true);
    }

    @Transactional
    public void delete(long userId, long id) {
        remover.remove(find(userId, id), "deleted by you");
    }

    /** Only finds the job if it belongs to this user. */
    public Job find(long userId, long id) {
        return jobs.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }
}
