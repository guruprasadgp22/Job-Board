package com.guru.jobboard.security;

import com.guru.jobboard.entity.Role;

/** The logged-in user, taken from the JWT. Inject with {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(Long id, String email, Role role) {
}
