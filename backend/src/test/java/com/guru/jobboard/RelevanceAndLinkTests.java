package com.guru.jobboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.service.LinkChecker;
import com.guru.jobboard.service.RelevanceScorer;
import com.guru.jobboard.service.SkillCatalog;
import com.guru.jobboard.source.FetchedJob;
import com.guru.jobboard.source.SourceUtils;

class RelevanceAndLinkTests {

    private final SkillCatalog catalog = new SkillCatalog();
    private final RelevanceScorer scorer = new RelevanceScorer(catalog);

    private static FetchedJob job(String title, String description, boolean remote) {
        return new FetchedJob("test", "1", title, "Acme", "Bangalore, India", remote, "https://x", description,
                List.of(), null, null);
    }

    private static Preference pref(String keywords) {
        Preference p = new Preference(1L);
        p.setKeywords(keywords);
        return p;
    }

    @Test
    void titleMatchScoresHigherThanDescriptionMatch() {
        Preference p = pref("java, spring boot");
        int inTitle = scorer.score(job("Java Spring Boot Developer", "", false), p).score();
        int inDescription = scorer.score(job("Backend Developer", "We use Java and Spring Boot", false), p).score();
        assertEquals(100, inTitle);
        assertTrue(inDescription > 0 && inDescription < inTitle);
    }

    @Test
    void javaDoesNotMatchJavascript() {
        assertEquals(0, scorer.score(job("JavaScript Engineer", "javascript only", false), pref("java")).score());
    }

    @Test
    void excludedWordsAndRemoteOnlySkipJob() {
        Preference p = pref("java");
        p.setExcludeKeywords("senior");
        assertEquals(-1, scorer.score(job("Senior Java Engineer", "", true), p).score());
        p.setExcludeKeywords("");
        p.setRemoteOnly(true);
        assertEquals(-1, scorer.score(job("Java Engineer", "", false), p).score());
    }

    @Test
    void resumeSkillsGiveMatchedAndMissing() {
        Preference p = pref("");
        p.setResumeSkills("Java, Spring Boot, React, PostgreSQL");
        RelevanceScorer.Match m = scorer.score(job("Backend Engineer",
                "Requirements: Java 17, Spring Boot, PostgreSQL and Kubernetes.", false), p);
        assertEquals(List.of("Java", "Spring Boot", "PostgreSQL"), m.matched().stream().sorted(
                (a, b) -> List.of("Java", "Spring Boot", "PostgreSQL").indexOf(a)
                        - List.of("Java", "Spring Boot", "PostgreSQL").indexOf(b)).toList());
        assertEquals(List.of("Kubernetes"), m.missing());
        assertEquals(60, m.score()); // 3 of 4 job skills, counted out of at least 5
    }

    @Test
    void extractsSkillsFromResumeText() {
        List<String> skills = catalog.extractRanked("""
                Skills: Java, Spring Boot, ReactJS, Node.js, PostgreSQL, Docker, REST APIs, C++
                Built microservices with Spring Boot and Kafka. Frontend in React + Redux. JavaScript daily.
                """);
        assertTrue(skills.containsAll(List.of("Java", "Spring Boot", "React", "Node.js", "PostgreSQL", "Docker",
                "REST API", "C++", "Microservices", "Kafka", "Redux", "JavaScript")), skills.toString());
        assertFalse(skills.contains("Spring"), "plain Spring is implied by Spring Boot");
        assertEquals("Spring Boot", skills.get(0));
    }

    @Test
    void normalizesTypedSkills() {
        assertEquals(List.of("Kubernetes", "React", "My Custom Skill"),
                catalog.normalize(List.of("k8s", "reactjs", "react", "My Custom Skill", " ")));
    }

    @Test
    void desiredRoleWorksWithoutKeywordsAndBoostsTitles() {
        Preference p = pref("");
        p.setDesiredRole("Java Developer");
        assertTrue(scorer.score(job("Senior Java Developer", "", false), p).score() > 0);
        assertEquals(0, scorer.score(job("Python Engineer", "", false), p).score());
    }

    @Test
    void experienceAdjustsSeniorAndJuniorTitles() {
        Preference p = pref("java");
        int base = scorer.score(job("Senior Java Engineer", "", false), p).score();
        p.setExperienceYears(2);
        assertEquals(base - 20, scorer.score(job("Senior Java Engineer", "", false), p).score());
        p.setExperienceYears(6);
        assertEquals(base, scorer.score(job("Senior Java Engineer", "", false), p).score());
        assertEquals(base - 15, scorer.score(job("Java Intern", "", false), p).score());
    }

    @Test
    void closedPagesAreDetected() {
        assertEquals(LinkChecker.Result.CLOSED,
                LinkChecker.classify("<h1>Sorry</h1><p>This job is <b>no longer accepting applications</b>.</p>"));
        assertEquals(LinkChecker.Result.OPEN, LinkChecker.classify("<h1>Java Developer</h1><button>Apply now</button>"));
    }

    @Test
    void htmlIsConvertedToText() {
        assertEquals("Hello\n• one\n• two", SourceUtils.htmlToText("<p>Hello</p><ul><li>one</li><li>two</li></ul>"));
    }
}
