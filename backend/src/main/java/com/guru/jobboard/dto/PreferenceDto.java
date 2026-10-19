package com.guru.jobboard.dto;

import com.guru.jobboard.entity.Preference;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record PreferenceDto(
        @Size(max = 1000) String keywords,
        @Size(max = 1000) String excludeKeywords,
        @Size(max = 200) String location,
        @Size(max = 5) String country,
        boolean remoteOnly,
        @Min(0) @Max(100) int minScore) {

    public static PreferenceDto from(Preference p) {
        return new PreferenceDto(p.getKeywords(), p.getExcludeKeywords(), p.getLocation(), p.getCountry(),
                p.isRemoteOnly(), p.getMinScore());
    }
}
