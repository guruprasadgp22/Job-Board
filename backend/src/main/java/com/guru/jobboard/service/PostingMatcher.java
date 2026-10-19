package com.guru.jobboard.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.guru.jobboard.entity.Posting;

/**
 * Scores a resume against a posting on this site: the share of the posting's skills (the ones the employer listed
 * plus any named in the title or description) that are on the resume. The same number is shown to the seeker
 * before applying and to the employer when reviewing applicants.
 */
@Component
public class PostingMatcher {

    public record Match(int score, List<String> matched, List<String> missing) {
    }

    private final SkillCatalog catalog;

    public PostingMatcher(SkillCatalog catalog) {
        this.catalog = catalog;
    }

    public Match match(Posting posting, String resumeSkillsCsv) {
        List<String> jobSkills = skillsOf(posting);
        Set<String> mine = catalog.withImplied(split(resumeSkillsCsv));
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String s : jobSkills) {
            (mine.contains(s) ? matched : missing).add(s);
        }
        int score = jobSkills.isEmpty() ? 0 : Math.round(100f * matched.size() / jobSkills.size());
        return new Match(score, matched, missing);
    }

    /** The employer's listed skills first, then others found in the title and description; no duplicates. */
    public List<String> skillsOf(Posting posting) {
        Map<String, String> byLower = new LinkedHashMap<>();
        for (String s : catalog.normalize(split(posting.getSkills()))) {
            byLower.putIfAbsent(s.toLowerCase(Locale.ROOT), s);
        }
        for (String s : catalog.extract(posting.getTitle() + "\n" + posting.getDescription())) {
            byLower.putIfAbsent(s.toLowerCase(Locale.ROOT), s);
        }
        return List.copyOf(byLower.values());
    }

    /** Canonical skill names ("reactjs" → React) as a comma-separated string, for storing on a posting. */
    public String normalizeSkills(String csv) {
        return String.join(", ", catalog.normalize(split(csv)));
    }

    static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
