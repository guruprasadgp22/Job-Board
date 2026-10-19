package com.guru.jobboard.dto;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** What an employer fills in to post or edit a job. */
public record PostingRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 200) String location,
        boolean remote,
        @Size(max = 30) String employmentType,
        @Min(0) @Max(60) Integer minExperience,
        @Min(0) @Max(60) Integer maxExperience,
        @Size(max = 100) String salary,
        @Size(max = 1000) String skills,
        @NotBlank @Size(max = 20_000) String description,
        Instant deadline) {
}
