package com.guru.jobboard.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.dto.PageDto;
import com.guru.jobboard.dto.PostingDto;
import com.guru.jobboard.dto.PostingRequest;
import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.JobApplication;
import com.guru.jobboard.entity.Posting;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.JobApplicationRepository;
import com.guru.jobboard.repository.PostingRepository;
import com.guru.jobboard.repository.UserRepository;

import jakarta.persistence.criteria.Predicate;

/** Employers create and manage their postings; job seekers browse the open ones. */
@Service
public class PostingService {

    private final PostingRepository postings;
    private final JobApplicationRepository applications;
    private final UserRepository users;
    private final PreferenceService preferences;
    private final PostingMatcher matcher;

    public PostingService(PostingRepository postings, JobApplicationRepository applications, UserRepository users,
                          PreferenceService preferences, PostingMatcher matcher) {
        this.postings = postings;
        this.applications = applications;
        this.users = users;
        this.preferences = preferences;
        this.matcher = matcher;
    }

    // ---------- Employer ----------

    @Transactional(readOnly = true)
    public List<PostingDto> listForEmployer(long employerId) {
        List<Posting> mine = postings.findByEmployerIdOrderByCreatedAtDesc(employerId);
        List<Long> ids = mine.stream().map(Posting::getId).toList();
        Map<Long, List<JobApplication>> byPosting = (ids.isEmpty() ? List.<JobApplication>of() : applications.findByPostingIdIn(ids))
                .stream().collect(Collectors.groupingBy(JobApplication::getPostingId));
        return mine.stream().map(p -> employerDto(p, byPosting.getOrDefault(p.getId(), List.of()), false)).toList();
    }

    @Transactional(readOnly = true)
    public PostingDto getForEmployer(long employerId, long id) {
        Posting p = owned(employerId, id);
        return employerDto(p, applications.findByPostingIdIn(List.of(id)), true);
    }

    @Transactional
    public PostingDto create(long employerId, PostingRequest req) {
        User employer = users.findById(employerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in"));
        if (employer.getOrganization() == null || employer.getOrganization().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add your organization's name in your profile first");
        }
        Posting p = new Posting();
        p.setEmployerId(employerId);
        p.setOrganization(employer.getOrganization());
        apply(p, req);
        return employerDto(postings.save(p), List.of(), true);
    }

    @Transactional
    public PostingDto update(long employerId, long id, PostingRequest req) {
        Posting p = owned(employerId, id);
        apply(p, req);
        p.setUpdatedAt(Instant.now());
        return employerDto(p, applications.findByPostingIdIn(List.of(id)), true);
    }

    /** Closing hides the posting from job seekers and stops new applications; existing ones are kept. */
    @Transactional
    public PostingDto setOpen(long employerId, long id, boolean open) {
        Posting p = owned(employerId, id);
        p.setOpen(open);
        p.setUpdatedAt(Instant.now());
        return employerDto(p, applications.findByPostingIdIn(List.of(id)), true);
    }

    /** Deletes the posting and every application to it. */
    @Transactional
    public void delete(long employerId, long id) {
        Posting p = owned(employerId, id);
        applications.deleteAllForPosting(p.getId());
        postings.delete(p);
    }

    public Posting owned(long employerId, long id) {
        return postings.findByIdAndEmployerId(id, employerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job post not found"));
    }

    private void apply(Posting p, PostingRequest req) {
        if (req.minExperience() != null && req.maxExperience() != null && req.minExperience() > req.maxExperience()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum experience is more than the maximum");
        }
        p.setTitle(req.title().trim());
        p.setLocation(clean(req.location()));
        p.setRemote(req.remote());
        p.setEmploymentType(clean(req.employmentType()));
        p.setMinExperience(req.minExperience());
        p.setMaxExperience(req.maxExperience());
        p.setSalary(clean(req.salary()));
        p.setSkills(JobSyncService.truncate(matcher.normalizeSkills(req.skills()), 1000));
        p.setDescription(req.description().trim());
        p.setDeadline(req.deadline());
    }

    private static PostingDto employerDto(Posting p, List<JobApplication> apps, boolean withDescription) {
        long fresh = apps.stream().filter(a -> a.getStatus() == ApplicantStatus.APPLIED).count();
        return PostingDto.forEmployer(p, withDescription, apps.size(), fresh);
    }

    // ---------- Job seeker ----------

    /** Open postings, best match first (or newest first), with this seeker's match and application status. */
    @Transactional(readOnly = true)
    public PageDto<PostingDto> browse(long seekerId, String q, Boolean remote, String sort, int page, int size) {
        Instant now = Instant.now();
        Specification<Posting> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isTrue(root.get("open")));
            p.add(cb.or(cb.isNull(root.get("deadline")), cb.greaterThan(root.get("deadline"), now)));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("organization")), like),
                        cb.like(cb.lower(root.get("location")), like),
                        cb.like(cb.lower(root.get("skills")), like)));
            }
            if (remote != null) {
                p.add(cb.equal(root.get("remote"), remote));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        List<Posting> all = postings.findAll(spec);
        String resumeSkills = preferences.get(seekerId).getResumeSkills();
        List<Long> ids = all.stream().map(Posting::getId).toList();
        Map<Long, ApplicantStatus> mine = (ids.isEmpty() ? List.<JobApplication>of()
                : applications.findBySeekerIdAndPostingIdIn(seekerId, ids)).stream()
                .collect(Collectors.toMap(JobApplication::getPostingId, JobApplication::getStatus));

        List<PostingDto> scored = all.stream().map(p -> seekerDto(p, resumeSkills, mine.get(p.getId()), false)).toList();
        Comparator<PostingDto> newest = Comparator.comparing(PostingDto::createdAt, Comparator.reverseOrder());
        Comparator<PostingDto> order = "newest".equals(sort) ? newest
                : Comparator.comparing(PostingDto::matchScore, Comparator.reverseOrder()).thenComparing(newest);
        List<PostingDto> sorted = scored.stream().sorted(order).toList();

        int pageSize = Math.clamp(size, 1, 100);
        int from = Math.min(Math.max(page, 0) * pageSize, sorted.size());
        int to = Math.min(from + pageSize, sorted.size());
        int totalPages = (sorted.size() + pageSize - 1) / pageSize;
        return new PageDto<>(sorted.subList(from, to), Math.max(page, 0), pageSize, sorted.size(), totalPages);
    }

    /** A posting seen by a seeker: open ones, or closed ones they already applied to. */
    @Transactional(readOnly = true)
    public PostingDto getForSeeker(long seekerId, long id) {
        Posting p = postings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        ApplicantStatus status = applications.findByPostingIdAndSeekerId(id, seekerId)
                .map(JobApplication::getStatus).orElse(null);
        if (!p.isOpen() && status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This job is no longer open");
        }
        return seekerDto(p, preferences.get(seekerId).getResumeSkills(), status, true);
    }

    private PostingDto seekerDto(Posting p, String resumeSkills, ApplicantStatus status, boolean withDescription) {
        PostingMatcher.Match m = matcher.match(p, resumeSkills);
        return PostingDto.forSeeker(p, withDescription, m.score(), String.join(", ", m.matched()),
                String.join(", ", m.missing()), status);
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
