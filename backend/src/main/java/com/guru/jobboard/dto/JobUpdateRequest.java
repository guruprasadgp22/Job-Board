package com.guru.jobboard.dto;

import com.guru.jobboard.entity.ApplicationStatus;

/** Fields left null are not changed. */
public record JobUpdateRequest(ApplicationStatus status, String notes) {
}
