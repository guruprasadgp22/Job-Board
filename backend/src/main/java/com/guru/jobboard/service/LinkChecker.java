package com.guru.jobboard.service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.guru.jobboard.config.HttpConfig;

/** Opens a job's page and decides whether the posting still accepts applications. */
@Component
public class LinkChecker {

    public enum Result { OPEN, CLOSED, UNKNOWN }

    private static final Logger log = LoggerFactory.getLogger(LinkChecker.class);
    private static final int MAX_BYTES = 400_000;

    static final List<String> CLOSED_PHRASES = List.of(
            "no longer accepting applications",
            "not accepting applications",
            "no longer accepting candidates",
            "this job has expired",
            "this job is expired",
            "job has expired",
            "this job posting has expired",
            "this posting has expired",
            "this listing has expired",
            "job listing has expired",
            "this job is no longer available",
            "job is no longer available",
            "this job is no longer active",
            "this position is no longer available",
            "position has been filled",
            "this position has been filled",
            "this job has been closed",
            "this job is closed",
            "job is closed",
            "applications are closed",
            "applications have closed",
            "the job you are looking for is no longer",
            "the job you're looking for is no longer",
            "this vacancy has expired",
            "this vacancy is closed",
            "vacancy is no longer available",
            "this role has been filled",
            "this role is no longer available",
            "sorry, this job is not available",
            // site-specific wording (LinkedIn, Indeed, Naukri, Wellfound)
            "this job has expired on indeed",
            "the employer is not accepting applications",
            "this job is no longer accepting applications",
            "job you are trying to apply for has been expired",
            "this job post has been expired",
            "this job is no longer open",
            "this position is closed");

    private final HttpClient http;

    public LinkChecker(HttpClient http) {
        this.http = http;
    }

    public Result check(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", HttpConfig.USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,*/*")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
            int code = response.statusCode();
            String body;
            try (InputStream in = response.body()) {
                body = new String(in.readNBytes(MAX_BYTES), StandardCharsets.UTF_8);
            }
            if (code == 404 || code == 410) {
                return Result.CLOSED;
            }
            if (code != 200) {
                // 403/429/5xx: blocked or flaky, never delete on those.
                return Result.UNKNOWN;
            }
            return classify(body);
        } catch (Exception e) {
            log.debug("Link check failed for {}: {}", url, e.toString());
            return Result.UNKNOWN;
        }
    }

    public static Result classify(String html) {
        String text = html.toLowerCase(Locale.ROOT)
                .replaceAll("<script[\\s\\S]*?</script>", " ")
                .replaceAll("<style[\\s\\S]*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replace("&#39;", "'")
                .replace("&rsquo;", "'")
                .replace('’', '\'')
                .replaceAll("\\s+", " ");
        for (String phrase : CLOSED_PHRASES) {
            if (text.contains(phrase)) {
                return Result.CLOSED;
            }
        }
        return Result.OPEN;
    }
}
