package com.guru.jobboard.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SkillsRequest(@NotNull @Size(max = 100) List<String> skills) {
}
