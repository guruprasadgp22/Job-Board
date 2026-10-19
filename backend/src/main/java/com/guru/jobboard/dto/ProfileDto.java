package com.guru.jobboard.dto;

import java.time.Instant;

import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.entity.Role;
import com.guru.jobboard.entity.User;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Everything on the profile page: personal details, career info and job-search preferences. */
public record ProfileDto(
        // Personal
        @NotBlank @Size(max = 100) String name,
        String email,
        /** Employers only. */
        @Size(max = 150) String organization,
        @Size(max = 30) String phone,
        @Size(max = 100) String currentCity,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 300) String githubUrl,
        @Size(max = 300) String portfolioUrl,
        @Size(max = 5000) String about,
        // Career
        @Size(max = 150) String headline,
        @Size(max = 150) String currentCompany,
        @Min(0) @Max(60) Integer experienceYears,
        @Size(max = 60) String expectedSalary,
        @Size(max = 60) String noticePeriod,
        // What to look for
        @Size(max = 150) String desiredRole,
        @Size(max = 1000) String keywords,
        @Size(max = 1000) String excludeKeywords,
        @Size(max = 200) String location,
        @Size(max = 5) String country,
        boolean remoteOnly,
        @Min(0) @Max(100) int minScore,
        // Read-only info
        Instant memberSince,
        Instant lastSyncAt,
        Role role) {

    public static ProfileDto from(User u, Preference p) {
        return new ProfileDto(u.getName(), u.getEmail(), u.getOrganization(), u.getPhone(), u.getCurrentCity(), u.getLinkedinUrl(),
                u.getGithubUrl(), u.getPortfolioUrl(), u.getAbout(), u.getHeadline(), u.getCurrentCompany(),
                p.getExperienceYears(), u.getExpectedSalary(), u.getNoticePeriod(), p.getDesiredRole(),
                p.getKeywords(), p.getExcludeKeywords(), p.getLocation(), p.getCountry(), p.isRemoteOnly(),
                p.getMinScore(), u.getCreatedAt(), p.getLastSyncAt(), u.getRole());
    }
}
