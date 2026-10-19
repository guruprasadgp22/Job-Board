package com.guru.jobboard.dto;

import java.time.Instant;

import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.entity.Job;

public record JobDto(
        Long id,
        String source,
        String title,
        String company,
        String location,
        boolean remote,
        String url,
        String description,
        String tags,
        String salary,
        Instant postedAt,
        Instant expiresAt,
        boolean addedManually,
        Instant firstSeenAt,
        Instant lastSeenAt,
        Instant lastCheckedAt,
        int relevanceScore,
        String matchedSkills,
        String missingSkills,
        ApplicationStatus status,
        String notes,
        Instant appliedAt) {

    public static JobDto from(Job j, boolean withDescription) {
        return new JobDto(j.getId(), j.getSource(), j.getTitle(), j.getCompany(), j.getLocation(), j.isRemote(),
                j.getUrl(), withDescription ? j.getDescription() : null, j.getTags(), j.getSalary(), j.getPostedAt(),
                j.getExpiresAt(), j.isAddedManually(),
                j.getFirstSeenAt(), j.getLastSeenAt(), j.getLastCheckedAt(), j.getRelevanceScore(), j.getMatchedSkills(), j.getMissingSkills(), j.getStatus(),
                j.getNotes(), j.getAppliedAt());
    }
}
