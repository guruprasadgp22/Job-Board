package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.entity.Preference;

/** https://remoteok.com/api — free, no key. Returns the latest ~100 remote jobs; the first element is a legal notice. */
@Component
public class RemoteOkSource implements JobSource {

    private final RestClient http;

    public RemoteOkSource(RestClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return "remoteok";
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        JsonNode root = http.get().uri("https://remoteok.com/api").retrieve().body(JsonNode.class);
        List<FetchedJob> jobs = new ArrayList<>();
        for (JsonNode j : root) {
            if (!j.hasNonNull("id") || !j.hasNonNull("position")) {
                continue;
            }
            int min = j.path("salary_min").asInt(0);
            int max = j.path("salary_max").asInt(0);
            String salary = max > 0 ? "$" + min / 1000 + "k – $" + max / 1000 + "k" : null;
            jobs.add(new FetchedJob(
                    name(),
                    j.path("id").asText(),
                    text(j, "position"),
                    text(j, "company"),
                    text(j, "location"),
                    true,
                    text(j, "url"),
                    htmlToText(text(j, "description")),
                    stringList(j.path("tags")),
                    salary,
                    parseDate(text(j, "date"))));
        }
        return jobs;
    }
}
