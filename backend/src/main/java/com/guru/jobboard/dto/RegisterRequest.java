package com.guru.jobboard.dto;

import com.guru.jobboard.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code role} defaults to SEEKER; {@code organization} is required for EMPLOYER. */
public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password,
        Role role,
        @Size(max = 150) String organization) {
}
