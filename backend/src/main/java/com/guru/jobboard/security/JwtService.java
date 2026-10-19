package com.guru.jobboard.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.entity.Role;
import com.guru.jobboard.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final SecretKey key;
    private final Duration validity;

    public JwtService(AppProperties props) {
        byte[] secret;
        if (props.jwtSecret() == null || props.jwtSecret().isBlank()) {
            log.warn("JWT_SECRET is not set: using a random key, so everyone is logged out when the app restarts.");
            secret = new byte[32];
            new SecureRandom().nextBytes(secret);
        } else {
            secret = props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(sha256(secret));
        this.validity = Duration.ofDays(Math.max(1, props.jwtDays()));
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(validity)))
                .signWith(key)
                .compact();
    }

    /** Returns null when the token is invalid or expired. */
    public AuthUser parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            // Tokens issued before roles existed have no role claim; those users are job seekers.
            String role = claims.get("role", String.class);
            return new AuthUser(Long.valueOf(claims.getSubject()), claims.get("email", String.class),
                    role == null ? Role.SEEKER : Role.valueOf(role));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
