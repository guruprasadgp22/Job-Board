package com.guru.jobboard.service;

import java.net.URI;
import java.util.Locale;

/** Maps job-site URLs and publisher names to short source names like "linkedin" or "naukri". */
public final class JobSites {

    private JobSites() {
    }

    /** "https://www.linkedin.com/jobs/view/123" → "linkedin"; unknown sites → their domain, e.g. "careers.acme.com". */
    public static String fromUrl(String url) {
        String host;
        try {
            host = URI.create(url.trim()).getHost();
        } catch (Exception e) {
            host = null;
        }
        if (host == null) {
            return "other";
        }
        host = host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "");
        String known = known(host);
        return known != null ? known : truncate(host);
    }

    /** JSearch publisher name ("LinkedIn", "Naukri.com", "Indeed") → source name. */
    public static String fromPublisher(String publisher) {
        if (publisher == null || publisher.isBlank()) {
            return "jsearch";
        }
        String lower = publisher.toLowerCase(Locale.ROOT);
        String known = known(lower);
        if (known != null) {
            return known;
        }
        String slug = lower.replaceAll("[^a-z0-9.]+", "-").replaceAll("^-|-$", "");
        return slug.isEmpty() ? "jsearch" : truncate(slug);
    }

    private static String known(String s) {
        if (s.contains("linkedin")) return "linkedin";
        if (s.contains("naukri")) return "naukri";
        if (s.contains("indeed")) return "indeed";
        if (s.contains("wellfound") || s.contains("angel.co") || s.contains("angellist")) return "wellfound";
        if (s.contains("glassdoor")) return "glassdoor";
        if (s.contains("foundit") || s.contains("monsterindia")) return "foundit";
        if (s.contains("instahyre")) return "instahyre";
        if (s.contains("ziprecruiter")) return "ziprecruiter";
        if (s.contains("remotive")) return "remotive";
        if (s.contains("remoteok")) return "remoteok";
        if (s.contains("arbeitnow")) return "arbeitnow";
        if (s.contains("themuse")) return "themuse";
        if (s.contains("adzuna")) return "adzuna";
        return null;
    }

    private static String truncate(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40);
    }
}
