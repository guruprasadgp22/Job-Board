package com.guru.jobboard.entity;

public enum ApplicationStatus {
    NEW,
    SAVED,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED;

    /** Jobs you've acted on are only removed when the posting itself is confirmed closed. */
    public boolean isActive() {
        return this == APPLIED || this == INTERVIEWING || this == OFFER;
    }
}
