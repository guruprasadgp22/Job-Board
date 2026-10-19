package com.guru.jobboard.dto;

import java.time.Instant;

import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.Posting;

/**
 * A posting as shown to employers (with applicant counts) or to job seekers (with their match and whether they
 * already applied). Fields that don't apply to the viewer are null.
 */
public record PostingDto(
        Long id,
        String organization,
        String title,
        String location,
        boolean remote,
        String employmentType,
        Integer minExperience,
        Integer maxExperience,
        String salary,
        String skills,
        String description,
        Instant deadline,
        boolean open,
        boolean acceptingApplications,
        Instant createdAt,
        Instant updatedAt,
        // Employer view
        Long applicantCount,
        Long newApplicantCount,
        // Seeker view
        Integer matchScore,
        String matchedSkills,
        String missingSkills,
        ApplicantStatus myStatus) {

    public static PostingDto forEmployer(Posting p, boolean withDescription, long applicants, long newApplicants) {
        return new PostingDto(p.getId(), p.getOrganization(), p.getTitle(), p.getLocation(), p.isRemote(),
                p.getEmploymentType(), p.getMinExperience(), p.getMaxExperience(), p.getSalary(), p.getSkills(),
                withDescription ? p.getDescription() : null, p.getDeadline(), p.isOpen(), p.acceptsApplications(),
                p.getCreatedAt(), p.getUpdatedAt(), applicants, newApplicants, null, null, null, null);
    }

    public static PostingDto forSeeker(Posting p, boolean withDescription, int matchScore, String matched,
                                       String missing, ApplicantStatus myStatus) {
        return new PostingDto(p.getId(), p.getOrganization(), p.getTitle(), p.getLocation(), p.isRemote(),
                p.getEmploymentType(), p.getMinExperience(), p.getMaxExperience(), p.getSalary(), p.getSkills(),
                withDescription ? p.getDescription() : null, p.getDeadline(), p.isOpen(), p.acceptsApplications(),
                p.getCreatedAt(), p.getUpdatedAt(), null, null, matchScore, matched, missing, myStatus);
    }
}
