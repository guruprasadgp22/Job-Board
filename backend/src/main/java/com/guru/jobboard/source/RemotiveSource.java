package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.entity.Preference;

/** https://remotive.com/api/remote-jobs — free, no key. Remote jobs only. */
@Component
public class RemotiveSource implements JobSource {

    private final RestClient http;

    public RemotiveSource(RestClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return "remotive";
    }

    @Override
    public String cacheKey(Preference pref) {
        return String.join(",", searchTerms(pref, 3)).toLowerCase();
    }

    @Override
    public boolean completeListing() {
        return true;
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        List<FetchedJob> jobs = new ArrayList<>();
        for (String term : searchTerms(pref, 3)) {
            JsonNode root = http.get()
                    .uri("https://remotive.com/api/remote-jobs?limit=100&search={q}", term)
                    .retrieve()
                    .body(JsonNode.class);
            for (JsonNode j : root.path("jobs")) {
                jobs.add(new FetchedJob(
                        name(),
                        j.path("id").asText(),
                        text(j, "title"),
                        text(j, "company_name"),
                        text(j, "candidate_required_location"),
                        true,
                        text(j, "url"),
                        htmlToText(text(j, "description")),
                        stringList(j.path("tags")),
                        text(j, "salary"),
                        parseDate(text(j, "publication_date"))));
            }
        }
        return jobs;
    }
}
