package com.guru.jobboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResumeTextRequest(@NotBlank @Size(max = 60_000) String text) {
}
