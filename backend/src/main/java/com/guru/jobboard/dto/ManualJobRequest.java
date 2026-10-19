package com.guru.jobboard.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A job you found yourself and pasted in. */
public record ManualJobRequest(
        @NotBlank @Size(max = 2000) @Pattern(regexp = "https?://.+", message = "must be an http(s) link") String url,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 300) String company,
        @Size(max = 300) String location,
        boolean remote,
        @Size(max = 200) String salary,
        String description,
        String notes,
        @Size(max = 1000) String tags,
        /** Application deadline, if the page states one. */
        Instant expiresAt) {
}
