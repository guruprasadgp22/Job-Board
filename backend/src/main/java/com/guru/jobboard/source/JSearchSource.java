package com.guru.jobboard.source;

import static com.guru.jobboard.source.SourceUtils.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.service.JobSites;

/**
 * JSearch (by OpenWeb Ninja, also sold on RapidAPI) returns Google for Jobs results, which include postings
 * from LinkedIn, Indeed, Naukri, Glassdoor and many company career pages, through an official API.
 * Each job is stored under its real publisher ("linkedin", "naukri", …).
 * <p>
 * Only active when JSEARCH_API_KEY is set. The free plan has a small monthly quota, so each distinct search
 * runs at most once every {@code min-hours} and asks for {@code pages} pages.
 */
@Component
public class JSearchSource implements JobSource {

    private final RestClient http;
    private final AppProperties.Jsearch config;
    private final Map<String, Instant> lastFetch = new ConcurrentHashMap<>();

    public JSearchSource(RestClient http, AppProperties props) {
        this.http = http;
        this.config = props.jsearch();
    }

    @Override
    public String name() {
        return "jsearch";
    }

    @Override
    public boolean enabled() {
        return config != null && config.enabled();
    }

    @Override
    public String cacheKey(Preference pref) {
        return (query(pref) + "|" + pref.getCountry()).toLowerCase(Locale.ROOT);
    }

    @Override
    public List<FetchedJob> fetch(Preference pref) {
        String key = cacheKey(pref);
        Instant last = lastFetch.get(key);
        if (last != null && last.isAfter(Instant.now().minus(Duration.ofHours(config.minHours())))) {
            return List.of(); // saves the API quota; jobs already stored stay as they are
        }
        String country = pref.getCountry() == null || pref.getCountry().isBlank() ? "in" : pref.getCountry().toLowerCase();
        boolean rapidApi = config.url().contains("rapidapi.com");
        JsonNode root = http.get()
                .uri(config.url() + "?query={q}&page=1&num_pages={pages}&country={country}&date_posted=month",
                        query(pref), Math.clamp(config.pages(), 1, 5), country)
                .headers(h -> {
                    if (rapidApi) {
                        h.set("x-rapidapi-key", config.apiKey());
                        h.set("x-rapidapi-host", java.net.URI.create(config.url()).getHost());
                    } else {
                        h.set("x-api-key", config.apiKey());
                    }
                })
                .retrieve()
                .body(JsonNode.class);
        lastFetch.put(key, Instant.now());
        return parse(root);
    }

    /** e.g. "Java Developer jobs in Bangalore" (desired role), else "Java Spring Boot jobs in Bangalore". */
    static String query(Preference pref) {
        String role = pref.getDesiredRole();
        String q = (role != null && !role.isBlank() ? role.trim() : String.join(" ", searchTerms(pref, 2))) + " jobs";
        if (pref.getLocation() != null && !pref.getLocation().isBlank()) {
            q += " in " + pref.getLocation().trim();
        }
        return q;
    }

    static List<FetchedJob> parse(JsonNode root) {
        List<FetchedJob> jobs = new ArrayList<>();
        for (JsonNode j : root.path("data")) {
            String publisher = text(j, "job_publisher");
            String source = JobSites.fromPublisher(publisher);
            String url = applyLink(j, publisher);
            if (url == null) {
                continue;
            }
            String location = firstNonBlank(text(j, "job_location"),
                    join(", ", text(j, "job_city"), text(j, "job_state"), text(j, "job_country")));
            String title = text(j, "job_title");
            String description = htmlToText(text(j, "job_description"));
            boolean remote = j.path("job_is_remote").asBoolean(false) || looksRemote(title, location);
            List<String> tags = new ArrayList<>();
            if (publisher != null) tags.add("via " + publisher);
            if (j.hasNonNull("job_employment_type")) tags.add(j.path("job_employment_type").asText());
            jobs.add(new FetchedJob(
                    source,
                    j.path("job_id").asText(),
                    title,
                    text(j, "employer_name"),
                    location,
                    remote,
                    url,
                    description,
                    tags,
                    salary(j),
                    parseDate(text(j, "job_posted_at_datetime_utc")),
                    parseDate(text(j, "job_offer_expiration_datetime_utc"))));
        }
        return jobs;
    }

    /** Prefer the apply link on the publisher's own site (e.g. the LinkedIn posting), else the default one. */
    private static String applyLink(JsonNode j, String publisher) {
        for (JsonNode option : j.path("apply_options")) {
            if (publisher != null && publisher.equalsIgnoreCase(option.path("publisher").asText())
                    && option.hasNonNull("apply_link")) {
                return option.path("apply_link").asText();
            }
        }
        return firstNonBlank(text(j, "job_apply_link"), text(j, "job_google_link"));
    }

    private static String salary(JsonNode j) {
        double min = j.path("job_min_salary").asDouble(0);
        double max = j.path("job_max_salary").asDouble(0);
        if (max <= 0) {
            return null;
        }
        String period = j.path("job_salary_period").asText("");
        String currency = j.path("job_salary_currency").asText("");
        return String.format("%s %,.0f – %,.0f%s", currency, min, max, period.isBlank() ? "" : " / " + period.toLowerCase())
                .trim();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String join(String sep, String... parts) {
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) out.add(p);
        }
        return String.join(sep, out);
    }
}
