package com.guru.jobboard.dto;

import java.util.Map;

public record SyncResult(int added, int updated, Map<String, String> sources) {
}
