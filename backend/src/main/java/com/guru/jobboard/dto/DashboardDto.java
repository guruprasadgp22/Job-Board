package com.guru.jobboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DashboardDto(
        String name,
        /** Setup checklist shown until everything is done. */
        List<Step> steps,
        int profileCompletion,
        long totalJobs,
        long newThisWeek,
        Map<String, Long> byStatus,
        List<JobDto> topMatches,
        List<JobDto> closingSoon,
        List<JobDto> recentApplications,
        /** Skills that many of your matched jobs ask for but your resume doesn't list. */
        List<SkillCount> skillsToLearn,
        /** Your skills that employers ask for most. */
        List<SkillCount> yourTopSkills,
        int averageMatch,
        Instant lastSyncAt,
        boolean hasResume) {

    public record Step(String key, String label, boolean done) {
    }

    public record SkillCount(String skill, long jobs) {
    }
}
