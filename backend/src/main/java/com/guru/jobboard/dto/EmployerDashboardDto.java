package com.guru.jobboard.dto;

import java.util.List;
import java.util.Map;

public record EmployerDashboardDto(
        String name,
        String organization,
        long openPostings,
        long closedPostings,
        long totalApplicants,
        long applicantsThisWeek,
        /** Applicants per status across all postings. */
        Map<String, Long> byStatus,
        List<ApplicantDto> recentApplicants,
        /** Open postings with the most applicants first. */
        List<PostingDto> topPostings,
        /** Open postings whose deadline is within 7 days. */
        List<PostingDto> closingSoon) {
}
