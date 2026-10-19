package com.guru.jobboard.dto;

import jakarta.validation.constraints.Size;

public record ApplyRequest(@Size(max = 3000) String coverNote) {
}
