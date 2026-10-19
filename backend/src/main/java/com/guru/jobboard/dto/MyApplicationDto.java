package com.guru.jobboard.dto;

import java.time.Instant;

import com.guru.jobboard.entity.ApplicantStatus;
import com.guru.jobboard.entity.JobApplication;
import com.guru.jobboard.entity.Posting;

/** A seeker's own application to a posting on this site. The employer's private notes are not included. */
public record MyApplicationDto(
        Long id,
        Long postingId,
        String title,
        String organization,
        String location,
        boolean remote,
        boolean postingOpen,
        ApplicantStatus status,
        int matchScore,
        Instant appliedAt,
        Instant updatedAt) {

    /** {@code posting} may be null if the employer deleted it. */
    public static MyApplicationDto from(JobApplication a, Posting posting) {
        boolean known = posting != null;
        return new MyApplicationDto(a.getId(), a.getPostingId(), known ? posting.getTitle() : "Job removed by employer",
                known ? posting.getOrganization() : null, known ? posting.getLocation() : null,
                known && posting.isRemote(), known && posting.acceptsApplications(), a.getStatus(), a.getMatchScore(),
                a.getAppliedAt(), a.getUpdatedAt());
    }
}
