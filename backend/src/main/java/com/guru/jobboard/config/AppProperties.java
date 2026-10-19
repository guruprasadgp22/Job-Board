package com.guru.jobboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String jwtSecret,
        int jwtDays,
        boolean allowRegistration,
        String cronSecret,
        Adzuna adzuna,
        Jsearch jsearch,
        Cleanup cleanup) {

    public record Adzuna(String appId, String appKey) {
        public boolean enabled() {
            return appId != null && !appId.isBlank() && appKey != null && !appKey.isBlank();
        }
    }

    /** JSearch: LinkedIn, Indeed, Naukri, Glassdoor… via Google for Jobs. */
    public record Jsearch(String apiKey, String url, int minHours, int pages) {
        public boolean enabled() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Cleanup(int maxAgeDays, int unlistedDays, int linkChecksPerRun) {
    }
}
