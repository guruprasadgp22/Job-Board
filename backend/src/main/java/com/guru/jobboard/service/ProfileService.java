package com.guru.jobboard.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.dto.ChangePasswordRequest;
import com.guru.jobboard.dto.ProfileDto;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.JobApplicationRepository;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.repository.PostingRepository;
import com.guru.jobboard.repository.PreferenceRepository;
import com.guru.jobboard.repository.RemovedJobRepository;
import com.guru.jobboard.repository.UserRepository;

@Service
public class ProfileService {

    private final UserRepository users;
    private final PreferenceService preferences;
    private final PreferenceRepository preferenceRepository;
    private final JobRepository jobs;
    private final RemovedJobRepository removedJobs;
    private final JobSyncService syncService;
    private final PasswordEncoder encoder;
    private final PostingRepository postings;
    private final JobApplicationRepository applications;

    public ProfileService(UserRepository users, PreferenceService preferences, PreferenceRepository preferenceRepository,
                          JobRepository jobs, RemovedJobRepository removedJobs, JobSyncService syncService,
                          PasswordEncoder encoder, PostingRepository postings, JobApplicationRepository applications) {
        this.postings = postings;
        this.applications = applications;
        this.users = users;
        this.preferences = preferences;
        this.preferenceRepository = preferenceRepository;
        this.jobs = jobs;
        this.removedJobs = removedJobs;
        this.syncService = syncService;
        this.encoder = encoder;
    }

    public ProfileDto get(long userId) {
        return ProfileDto.from(user(userId), preferences.get(userId));
    }

    @Transactional
    public ProfileDto update(long userId, ProfileDto dto) {
        User u = user(userId);
        u.setName(dto.name().trim());
        u.setPhone(clean(dto.phone()));
        u.setCurrentCity(clean(dto.currentCity()));
        u.setLinkedinUrl(clean(dto.linkedinUrl()));
        u.setGithubUrl(clean(dto.githubUrl()));
        u.setPortfolioUrl(clean(dto.portfolioUrl()));
        u.setAbout(clean(dto.about()));
        u.setHeadline(clean(dto.headline()));
        u.setCurrentCompany(clean(dto.currentCompany()));
        u.setExpectedSalary(clean(dto.expectedSalary()));
        u.setNoticePeriod(clean(dto.noticePeriod()));
        if (u.isEmployer()) {
            String organization = clean(dto.organization());
            if (organization == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your organization's name");
            }
            if (!organization.equals(u.getOrganization())) {
                u.setOrganization(organization);
                postings.renameOrganization(userId, organization);
            }
        }
        users.save(u);

        Preference p = preferences.get(userId);
        if (u.isEmployer()) {
            // Employers don't search for jobs, so their job-search settings are left alone.
            return ProfileDto.from(u, p);
        }
        p.setDesiredRole(clean(dto.desiredRole()));
        p.setExperienceYears(dto.experienceYears());
        p.setKeywords(dto.keywords() == null ? "" : dto.keywords().trim());
        p.setExcludeKeywords(dto.excludeKeywords() == null ? "" : dto.excludeKeywords().trim());
        p.setLocation(dto.location() == null ? "" : dto.location().trim());
        p.setCountry(dto.country() == null || dto.country().isBlank() ? "in" : dto.country().trim().toLowerCase());
        p.setRemoteOnly(dto.remoteOnly());
        p.setMinScore(dto.minScore());
        preferences.save(p);

        syncService.rescore(userId);
        return ProfileDto.from(u, p);
    }

    @Transactional
    public void changePassword(long userId, ChangePasswordRequest req) {
        User u = user(userId);
        if (!encoder.matches(req.currentPassword(), u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Your current password is wrong");
        }
        u.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(u);
    }

    /** Permanently deletes the account and everything stored for it. */
    @Transactional
    public void deleteAccount(long userId, String password) {
        User u = user(userId);
        if (password == null || !encoder.matches(password, u.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is wrong");
        }
        if (u.isEmployer()) {
            applications.deleteAllForEmployer(userId);
            postings.findByEmployerIdOrderByCreatedAtDesc(userId).forEach(postings::delete);
        } else {
            applications.deleteAllForSeeker(userId);
        }
        jobs.deleteAllForUser(userId);
        removedJobs.deleteAllForUser(userId);
        preferenceRepository.findByUserId(userId).ifPresent(preferenceRepository::delete);
        users.delete(u);
    }

    private User user(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in"));
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
