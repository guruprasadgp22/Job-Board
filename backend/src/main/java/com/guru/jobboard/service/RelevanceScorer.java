package com.guru.jobboard.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.source.FetchedJob;

/**
 * Scores a job 0-100 for one user.
 * <ul>
 *   <li><b>Keyword score:</b> each keyword earns 3 points in the title, 2 in the tags, 1 in the description.</li>
 *   <li><b>Resume score:</b> share of the skills the job mentions that are also on your resume
 *       (Spring Boot on your resume also covers "Spring").</li>
 *   <li>With both, the final score is the average. A preferred-location match adds 10.</li>
 *   <li><b>Desired role</b> in the title adds 10 (5 when only its key words appear). Without keywords, the
 *       role itself is the keyword.</li>
 *   <li><b>Experience:</b> senior/lead titles lose 20 points when you have under 4 years; intern/fresher titles
 *       lose 15 when you have 3+ years.</li>
 * </ul>
 * When keywords are set, at least one must appear somewhere in the job, so unrelated jobs aren't stored just
 * because they mention "Git".
 */
@Component
public class RelevanceScorer {

    public record Match(int score, List<String> matched, List<String> missing) {
        static final Match SKIP = new Match(-1, List.of(), List.of());
        static final Match NONE = new Match(0, List.of(), List.of());
    }

    private static final int MIN_SKILLS = 5;
    private static final Pattern SENIOR = Pattern.compile(
            "\\b(senior|sr\\.?|lead|principal|staff|architect|head of|director|manager|vp)\\b");
    private static final Pattern JUNIOR = Pattern.compile(
            "\\b(junior|jr\\.?|intern|internship|trainee|fresher|freshers|graduate|entry[- ]level)\\b");
    private static final List<String> GENERIC_ROLE_WORDS = List.of(
            "developer", "engineer", "software", "programmer", "specialist", "consultant", "and", "&", "-", "/");

    private final SkillCatalog catalog;

    public RelevanceScorer(SkillCatalog catalog) {
        this.catalog = catalog;
    }

    public Match score(FetchedJob job, Preference pref) {
        return score(job.title(), job.tags() == null ? "" : String.join(" ", job.tags()), job.description(),
                job.location(), job.remote(), pref);
    }

    public Match score(String titleRaw, String tagsRaw, String descriptionRaw, String locationRaw, boolean remote,
                       Preference pref) {
        String title = lower(titleRaw);
        String tags = lower(tagsRaw);
        String description = lower(descriptionRaw);

        for (String excluded : split(pref.getExcludeKeywords())) {
            if (contains(title, excluded)) {
                return Match.SKIP;
            }
        }
        if (pref.isRemoteOnly() && !remote) {
            return Match.SKIP;
        }

        String role = lower(pref.getDesiredRole()).trim();
        List<String> keywords = split(pref.getKeywords());
        if (keywords.isEmpty() && !role.isEmpty()) {
            keywords = List.of(role);
        }
        int keywordScore = -1;
        if (!keywords.isEmpty()) {
            int points = 0;
            for (String k : keywords) {
                if (contains(title, k)) {
                    points += 3;
                } else if (contains(tags, k)) {
                    points += 2;
                } else if (contains(description, k)) {
                    points += 1;
                }
            }
            if (points == 0) {
                return Match.NONE;
            }
            keywordScore = Math.round(100f * points / (3f * keywords.size()));
        }

        List<String> resumeSkills = split(pref.getResumeSkills());
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        int resumeScore = -1;
        if (!resumeSkills.isEmpty()) {
            Set<String> mine = catalog.withImplied(resumeSkills);
            Set<String> jobSkills = catalog.extract(titleRaw + "\n" + tagsRaw + "\n" + descriptionRaw);
            for (String s : jobSkills) {
                (mine.contains(s) ? matched : missing).add(s);
            }
            if (!jobSkills.isEmpty()) {
                // A job naming only 1-2 skills shouldn't reach 100% from them: count at least MIN_SKILLS.
                resumeScore = Math.round(100f * matched.size() / Math.max(jobSkills.size(), MIN_SKILLS));
            }
            if (keywordScore < 0 && matched.isEmpty()) {
                return new Match(0, matched, missing);
            }
        }

        int score;
        if (keywordScore >= 0 && resumeScore >= 0) {
            score = Math.round((keywordScore + resumeScore) / 2f);
        } else if (keywordScore >= 0) {
            score = keywordScore;
        } else if (resumeScore >= 0) {
            score = resumeScore;
        } else {
            return Match.NONE;
        }

        score += roleBonus(title, role) + experienceAdjustment(title, pref.getExperienceYears());
        score = Math.max(1, score);

        String location = lower(pref.getLocation()).trim();
        if (!location.isBlank() && lower(locationRaw).contains(location)) {
            score += 10;
        }
        return new Match(Math.min(100, score), matched, missing);
    }

    static int roleBonus(String title, String role) {
        if (role.isEmpty()) {
            return 0;
        }
        if (contains(title, role)) {
            return 10;
        }
        List<String> keyWords = Arrays.stream(role.split("\\s+"))
                .filter(w -> !w.isBlank() && !GENERIC_ROLE_WORDS.contains(w))
                .toList();
        return !keyWords.isEmpty() && keyWords.stream().allMatch(w -> contains(title, w)) ? 5 : 0;
    }

    static int experienceAdjustment(String title, Integer years) {
        if (years == null) {
            return 0;
        }
        if (years < 4 && SENIOR.matcher(title).find()) {
            return -20;
        }
        if (years >= 3 && JUNIOR.matcher(title).find()) {
            return -15;
        }
        return 0;
    }

    static List<String> split(String csv) {
        if (csv == null) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /** Whole-word match, so "java" does not match "javascript". */
    static boolean contains(String haystack, String keyword) {
        if (haystack.isEmpty() || !haystack.contains(keyword)) {
            return false;
        }
        return SkillCatalog.wordPattern(keyword).matcher(haystack).find();
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
