package com.guru.jobboard.dto;

import java.time.Instant;
import java.util.List;

public record ResumeDto(String fileName, String text, List<String> skills, Instant updatedAt, String keywords) {
}
