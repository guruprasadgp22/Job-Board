package com.guru.jobboard.dto;

import java.time.Instant;

import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.JobApplication;
import com.guru.jobboard.entity.User;

/** An application as the employer sees it: the candidate's contact details, resume and match. */
public record ApplicantDto(
        Long id,
        Long postingId,
        String postingTitle,
        ApplicantStatus status,
        Instant appliedAt,
        Instant updatedAt,
        int matchScore,
        String matchedSkills,
        String missingSkills,
        String coverNote,
        String employerNotes,
        // Candidate
        String name,
        String email,
        String phone,
        String headline,
        String currentCity,
        String currentCompany,
        Integer experienceYears,
        String noticePeriod,
        String expectedSalary,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl,
        String resumeFileName,
        String resumeSkills,
        String resumeText) {

    /** {@code seeker} may be null if the account was deleted in the meantime. */
    public static ApplicantDto from(JobApplication a, String postingTitle, User seeker, boolean withResume) {
        boolean known = seeker != null;
        return new ApplicantDto(a.getId(), a.getPostingId(), postingTitle, a.getStatus(), a.getAppliedAt(),
                a.getUpdatedAt(), a.getMatchScore(), a.getMatchedSkills(), a.getMissingSkills(), a.getCoverNote(),
                a.getEmployerNotes(),
                known ? seeker.getName() : "Deleted account", known ? seeker.getEmail() : null,
                known ? seeker.getPhone() : null, known ? seeker.getHeadline() : null,
                known ? seeker.getCurrentCity() : null, known ? seeker.getCurrentCompany() : null,
                a.getExperienceYears(), known ? seeker.getNoticePeriod() : null,
                known ? seeker.getExpectedSalary() : null, known ? seeker.getLinkedinUrl() : null,
                known ? seeker.getGithubUrl() : null, known ? seeker.getPortfolioUrl() : null,
                a.getResumeFileName(), a.getResumeSkills(), withResume ? a.getResumeText() : null);
    }
}
