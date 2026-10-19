package com.guru.jobboard.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.dto.ApplicantDto;
import com.guru.jobboard.dto.ApplicantUpdateRequest;
import com.guru.jobboard.dto.MyApplicationDto;
import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.JobApplication;
import com.guru.jobboard.entity.Posting;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.JobApplicationRepository;
import com.guru.jobboard.repository.PostingRepository;
import com.guru.jobboard.repository.UserRepository;

/** Job seekers apply to postings; employers review the applicants to their own postings. */
@Service
public class ApplicationService {

    private final JobApplicationRepository applications;
    private final PostingRepository postings;
    private final PostingService postingService;
    private final UserRepository users;
    private final PreferenceService preferences;
    private final PostingMatcher matcher;

    public ApplicationService(JobApplicationRepository applications, PostingRepository postings,
                              PostingService postingService, UserRepository users, PreferenceService preferences,
                              PostingMatcher matcher) {
        this.applications = applications;
        this.postings = postings;
        this.postingService = postingService;
        this.users = users;
        this.preferences = preferences;
        this.matcher = matcher;
    }

    // ---------- Job seeker ----------

    /** Sends the seeker's current resume to the employer. */
    @Transactional
    public MyApplicationDto apply(long seekerId, long postingId, String coverNote) {
        Posting posting = postings.findById(postingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
        if (!posting.acceptsApplications()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This job is no longer accepting applications");
        }
        if (applications.findByPostingIdAndSeekerId(postingId, seekerId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this job");
        }
        Preference pref = preferences.get(seekerId);
        if (pref.getResumeText() == null || pref.getResumeText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Upload your resume first (Profile → Resume & skills). It's sent with your application.");
        }
        PostingMatcher.Match m = matcher.match(posting, pref.getResumeSkills());
        JobApplication a = new JobApplication();
        a.setPostingId(postingId);
        a.setSeekerId(seekerId);
        a.setCoverNote(coverNote == null || coverNote.isBlank() ? null : coverNote.trim());
        a.setResumeText(pref.getResumeText());
        a.setResumeFileName(pref.getResumeFileName());
        a.setResumeSkills(pref.getResumeSkills());
        a.setExperienceYears(pref.getExperienceYears());
        a.setMatchScore(m.score());
        a.setMatchedSkills(JobSyncService.truncate(String.join(", ", m.matched()), 1000));
        a.setMissingSkills(JobSyncService.truncate(String.join(", ", m.missing()), 1000));
        try {
            return MyApplicationDto.from(applications.saveAndFlush(a), posting);
        } catch (DataIntegrityViolationException e) {
            // Two clicks at once: the unique (posting, seeker) constraint caught the second one.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already applied to this job");
        }
    }

    /** Withdraw is only possible before the employer has acted on the application. */
    @Transactional
    public void withdraw(long seekerId, long postingId) {
        JobApplication a = applications.findByPostingIdAndSeekerId(postingId, seekerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "You haven't applied to this job"));
        if (a.getStatus() != ApplicantStatus.APPLIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The employer is already reviewing your application, so it can't be withdrawn");
        }
        applications.delete(a);
    }

    @Transactional(readOnly = true)
    public List<MyApplicationDto> mine(long seekerId) {
        List<JobApplication> mine = applications.findBySeekerIdOrderByAppliedAtDesc(seekerId);
        Map<Long, Posting> byId = postings.findAllById(mine.stream().map(JobApplication::getPostingId).toList())
                .stream().collect(Collectors.toMap(Posting::getId, Function.identity()));
        return mine.stream().map(a -> MyApplicationDto.from(a, byId.get(a.getPostingId()))).toList();
    }

    // ---------- Employer ----------

    /** Applicants to one posting, best match first. */
    @Transactional(readOnly = true)
    public List<ApplicantDto> forPosting(long employerId, long postingId) {
        Posting posting = postingService.owned(employerId, postingId);
        return toDtos(applications.findByPostingIdOrderByMatchScoreDescAppliedAtAsc(postingId),
                Map.of(posting.getId(), posting), false);
    }

    /** Applicants across all of the employer's postings, newest first. */
    @Transactional(readOnly = true)
    public List<ApplicantDto> forEmployer(long employerId) {
        Map<Long, Posting> mine = postings.findByEmployerIdOrderByCreatedAtDesc(employerId).stream()
                .collect(Collectors.toMap(Posting::getId, Function.identity()));
        if (mine.isEmpty()) {
            return List.of();
        }
        List<JobApplication> all = applications.findByPostingIdIn(mine.keySet()).stream()
                .sorted(Comparator.comparing(JobApplication::getAppliedAt).reversed())
                .toList();
        return toDtos(all, mine, false);
    }

    @Transactional(readOnly = true)
    public ApplicantDto get(long employerId, long applicationId) {
        JobApplication a = owned(employerId, applicationId);
        Posting posting = postings.findById(a.getPostingId()).orElseThrow();
        return ApplicantDto.from(a, posting.getTitle(), users.findById(a.getSeekerId()).orElse(null), true);
    }

    @Transactional
    public ApplicantDto update(long employerId, long applicationId, ApplicantUpdateRequest req) {
        JobApplication a = owned(employerId, applicationId);
        if (req.status() != null) {
            a.setStatus(req.status());
        }
        if (req.employerNotes() != null) {
            a.setEmployerNotes(req.employerNotes().isBlank() ? null : req.employerNotes());
        }
        a.setUpdatedAt(Instant.now());
        Posting posting = postings.findById(a.getPostingId()).orElseThrow();
        return ApplicantDto.from(a, posting.getTitle(), users.findById(a.getSeekerId()).orElse(null), true);
    }

    /** Only finds the application if it is to one of this employer's postings. */
    private JobApplication owned(long employerId, long applicationId) {
        JobApplication a = applications.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        postings.findByIdAndEmployerId(a.getPostingId(), employerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return a;
    }

    List<ApplicantDto> toDtos(List<JobApplication> apps, Map<Long, Posting> postingsById, boolean withResume) {
        Map<Long, User> seekers = users.findAllById(apps.stream().map(JobApplication::getSeekerId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return apps.stream()
                .map(a -> ApplicantDto.from(a, postingsById.get(a.getPostingId()).getTitle(),
                        seekers.get(a.getSeekerId()), withResume))
                .toList();
    }
}
