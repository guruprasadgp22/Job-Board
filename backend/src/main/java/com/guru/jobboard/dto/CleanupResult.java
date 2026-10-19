package com.guru.jobboard.dto;

import java.util.List;

public record CleanupResult(int linksChecked, int closedByLinkCheck, int unlisted, int tooOld, int expired,
                            List<String> removed) {
}
