package com.guru.jobboard.source;

import java.time.Instant;
import java.util.List;

/** A job as returned by an external source, before scoring and saving. */
public record FetchedJob(
        String source,
        String externalId,
        String title,
        String company,
        String location,
        boolean remote,
        String url,
        String description,
        List<String> tags,
        String salary,
        Instant postedAt,
        /** When the posting stops accepting applications, if the source says so. */
        Instant expiresAt) {

    public FetchedJob(String source, String externalId, String title, String company, String location, boolean remote,
                      String url, String description, List<String> tags, String salary, Instant postedAt) {
        this(source, externalId, title, company, location, remote, url, description, tags, salary, postedAt, null);
    }
}
