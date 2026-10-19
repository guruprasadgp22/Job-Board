package com.guru.jobboard.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StatsDto(
        long total,
        Map<String, Long> byStatus,
        Map<String, Long> bySource,
        Map<String, Instant> lastSourceSuccess,
        List<String> enabledSources,
        boolean adzunaEnabled,
        boolean jsearchEnabled) {
}
