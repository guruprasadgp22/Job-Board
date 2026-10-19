package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.entity.Preference;

/** https://www.themuse.com/developers/api/v2 — free, no key needed. Software engineering category. */
@Component
public class TheMuseSource implements JobSource {

    private static final int PAGES = 5;

    private final RestClient http;

    public TheMuseSource(RestClient http) {
        this.http = http;
    }

    @Override
    public String name() {
        return "themuse";
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        List<FetchedJob> jobs = new ArrayList<>();
        for (int page = 0; page < PAGES; page++) {
            JsonNode root = http.get()
                    .uri("https://www.themuse.com/api/public/jobs?category={c}&descending=true&page={p}",
                            "Software Engineering", page)
                    .retrieve()
                    .body(JsonNode.class);
            for (JsonNode j : root.path("results")) {
                List<String> locations = stringList(j.path("locations"));
                String location = String.join(" / ", locations);
                List<String> tags = new ArrayList<>(stringList(j.path("levels")));
                tags.addAll(stringList(j.path("categories")));
                jobs.add(new FetchedJob(
                        name(),
                        j.path("id").asText(),
                        text(j, "name"),
                        j.path("company").path("name").asText(null),
                        location,
                        looksRemote(location),
                        j.path("refs").path("landing_page").asText(null),
                        htmlToText(text(j, "contents")),
                        tags,
                        null,
                        parseDate(text(j, "publication_date"))));
            }
        }
        return jobs;
    }
}
