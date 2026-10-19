package com.guru.jobboard.security;

/** The logged-in user, taken from the JWT. Inject with {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(Long id, String email) {
}
