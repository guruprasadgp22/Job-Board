package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.entity.Preference;

/**
 * https://developer.adzuna.com — free API key. Covers India (country "in") and ~20 other countries,
 * including on-site jobs. Only active when ADZUNA_APP_ID and ADZUNA_APP_KEY are set.
 */
@Component
public class AdzunaSource implements JobSource {

    private final RestClient http;
    private final AppProperties props;

    public AdzunaSource(RestClient http, AppProperties props) {
        this.http = http;
        this.props = props;
    }

    @Override
    public String name() {
        return "adzuna";
    }

    @Override
    public boolean enabled() {
        return props.adzuna() != null && props.adzuna().enabled();
    }

    @Override
    public String cacheKey(Preference pref) {
        return (String.join(",", searchTerms(pref, 3)) + "|" + pref.getCountry() + "|" + pref.getLocation())
                .toLowerCase();
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        String country = pref.getCountry() == null || pref.getCountry().isBlank() ? "in" : pref.getCountry().toLowerCase();
        String where = pref.getLocation() == null ? "" : pref.getLocation().trim();
        List<FetchedJob> jobs = new ArrayList<>();
        for (String term : searchTerms(pref, 3)) {
            JsonNode root = http.get()
                    .uri(b -> {
                        b.scheme("https").host("api.adzuna.com")
                                .path("/v1/api/jobs/{country}/search/1")
                                .queryParam("app_id", props.adzuna().appId())
                                .queryParam("app_key", props.adzuna().appKey())
                                .queryParam("results_per_page", 50)
                                .queryParam("max_days_old", 30)
                                .queryParam("sort_by", "date")
                                .queryParam("what", term);
                        if (!where.isEmpty()) {
                            b.queryParam("where", where);
                        }
                        return b.build(country);
                    })
                    .retrieve()
                    .body(JsonNode.class);
            for (JsonNode j : root.path("results")) {
                String location = j.path("location").path("display_name").asText(null);
                String title = htmlToText(text(j, "title"));
                String description = htmlToText(text(j, "description"));
                double min = j.path("salary_min").asDouble(0);
                double max = j.path("salary_max").asDouble(0);
                String salary = max > 0 ? String.format("%,.0f – %,.0f", min, max) : null;
                jobs.add(new FetchedJob(
                        name(),
                        j.path("id").asText(),
                        title,
                        j.path("company").path("display_name").asText(null),
                        location,
                        looksRemote(title, location, description),
                        text(j, "redirect_url"),
                        description,
                        List.of(j.path("category").path("label").asText("")),
                        salary,
                        parseDate(text(j, "created"))));
            }
        }
        return jobs;
    }
}
