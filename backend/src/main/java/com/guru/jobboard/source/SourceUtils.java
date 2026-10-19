package com.guru.jobboard.source;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.databind.JsonNode;

public final class SourceUtils {

    private static final int MAX_DESCRIPTION = 20_000;

    private SourceUtils() {
    }

    /** Converts HTML (possibly entity-escaped) into readable plain text. */
    public static String htmlToText(String html) {
        if (html == null) {
            return "";
        }
        String s = html.contains("&lt;") ? HtmlUtils.htmlUnescape(html) : html;
        s = s.replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</(p|div|li|h[1-6]|ul|ol)>", "\n")
                .replaceAll("(?i)<li[^>]*>", "• ")
                .replaceAll("<[^>]+>", "");
        s = HtmlUtils.htmlUnescape(s)
                .replace(' ', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .trim();
        return s.length() > MAX_DESCRIPTION ? s.substring(0, MAX_DESCRIPTION) + "…" : s;
    }

    public static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    public static List<String> stringList(JsonNode array) {
        List<String> out = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> out.add(n.isObject() ? n.path("name").asText() : n.asText()));
        }
        return out;
    }

    public static Instant parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        } catch (Exception ignored) {
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(value));
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Search queries: your desired role first, then your keywords (or top resume skills when no keywords are set). */
    public static List<String> searchTerms(com.guru.jobboard.entity.Preference pref, int max) {
        String keywords = pref.getKeywords();
        String base = keywords == null || keywords.isBlank() ? pref.getResumeSkills() : keywords;
        String role = pref.getDesiredRole();
        if (role != null && !role.isBlank()) {
            base = role.trim() + (base == null || base.isBlank() ? "" : ", " + base);
        }
        return searchTerms(base, max).stream().distinct().toList();
    }

    /** The first {@code max} comma-separated keywords, used as search queries. */
    public static List<String> searchTerms(String keywords, int max) {
        return Arrays.stream(keywords == null ? new String[0] : keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(max)
                .toList();
    }

    public static boolean looksRemote(String... values) {
        for (String v : values) {
            if (v != null && v.toLowerCase().matches(".*\\b(remote|anywhere|worldwide|work from home|wfh)\\b.*")) {
                return true;
            }
        }
        return false;
    }
}
