package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.entity.Preference;

/** https://www.arbeitnow.com/api/job-board-api — free, no key. Mostly Europe + remote. No search, so we filter by score. */
@Component
public class ArbeitnowSource implements JobSource {

    private static final int PAGES = 3;

    private final RestClient http;

    public ArbeitnowSource(RestClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return "arbeitnow";
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        List<FetchedJob> jobs = new ArrayList<>();
        for (int page = 1; page <= PAGES; page++) {
            JsonNode root = http.get()
                    .uri("https://www.arbeitnow.com/api/job-board-api?page={p}", page)
                    .retrieve()
                    .body(JsonNode.class);
            for (JsonNode j : root.path("data")) {
                jobs.add(new FetchedJob(
                        name(),
                        text(j, "slug"),
                        text(j, "title"),
                        text(j, "company_name"),
                        text(j, "location"),
                        j.path("remote").asBoolean(false),
                        text(j, "url"),
                        htmlToText(text(j, "description")),
                        stringList(j.path("tags")),
                        null,
                        parseDate(text(j, "created_at"))));
            }
            if (root.path("links").path("next").isNull()) {
                break;
            }
        }
        return jobs;
    }
}
