package com.guru.jobboard.dto;

import com.guru.jobboard.entity.ApplicantStatus;

import jakarta.validation.constraints.Size;

/** Fields left null are not changed. */
public record ApplicantUpdateRequest(ApplicantStatus status, @Size(max = 10_000) String employerNotes) {
}
