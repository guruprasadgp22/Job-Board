package com.guru.jobboard.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.dto.DashboardDto;
import com.guru.jobboard.dto.DashboardDto.SkillCount;
import com.guru.jobboard.dto.DashboardDto.Step;
import com.guru.jobboard.dto.JobDto;
import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.entity.Job;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.repository.UserRepository;

/** Builds each user's personal dashboard from their own jobs, profile and resume. */
@Service
public class DashboardService {

    private static final Set<ApplicationStatus> IN_PROGRESS =
            EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEWING, ApplicationStatus.OFFER);

    private final JobRepository jobs;
    private final UserRepository users;
    private final PreferenceService preferences;

    public DashboardService(JobRepository jobs, UserRepository users, PreferenceService preferences) {
        this.jobs = jobs;
        this.users = users;
        this.preferences = preferences;
    }

    @Transactional(readOnly = true)
    public DashboardDto build(long userId) {
        User user = users.findById(userId).orElseThrow();
        Preference pref = preferences.get(userId);
        List<Job> all = jobs.findByUserId(userId);
        Instant now = Instant.now();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        Arrays.stream(ApplicationStatus.values()).forEach(s -> byStatus.put(s.name(), 0L));
        all.forEach(j -> byStatus.merge(j.getStatus().name(), 1L, Long::sum));

        long newThisWeek = all.stream().filter(j -> j.getFirstSeenAt().isAfter(now.minus(Duration.ofDays(7)))).count();

        List<JobDto> topMatches = all.stream()
                .filter(j -> j.getStatus() == ApplicationStatus.NEW)
                .sorted(Comparator.comparingInt(Job::getRelevanceScore).reversed()
                        .thenComparing(Job::getFirstSeenAt, Comparator.reverseOrder()))
                .limit(5).map(j -> JobDto.from(j, false)).toList();

        List<JobDto> closingSoon = all.stream()
                .filter(j -> j.getExpiresAt() != null && j.getExpiresAt().isBefore(now.plus(Duration.ofDays(7))))
                .filter(j -> j.getStatus() != ApplicationStatus.REJECTED)
                .sorted(Comparator.comparing(Job::getExpiresAt))
                .limit(5).map(j -> JobDto.from(j, false)).toList();

        List<JobDto> recentApplications = all.stream()
                .filter(j -> IN_PROGRESS.contains(j.getStatus()))
                .sorted(Comparator.comparing((Job j) -> j.getAppliedAt() == null ? j.getFirstSeenAt() : j.getAppliedAt())
                        .reversed())
                .limit(5).map(j -> JobDto.from(j, false)).toList();

        Map<String, Long> missing = new HashMap<>();
        Map<String, Long> matched = new HashMap<>();
        for (Job j : all) {
            if (j.getStatus() == ApplicationStatus.REJECTED) {
                continue;
            }
            split(j.getMissingSkills()).forEach(s -> missing.merge(s, 1L, Long::sum));
            split(j.getMatchedSkills()).forEach(s -> matched.merge(s, 1L, Long::sum));
        }

        int averageMatch = all.isEmpty() ? 0
                : (int) Math.round(all.stream().mapToInt(Job::getRelevanceScore).average().orElse(0));

        boolean hasResume = pref.getResumeText() != null && !pref.getResumeText().isBlank();
        List<Step> steps = List.of(
                new Step("account", "Create your account", true),
                new Step("resume", "Upload your resume", hasResume),
                new Step("role", "Set the job title you want", notBlank(pref.getDesiredRole())),
                new Step("details", "Add experience and preferred location",
                        pref.getExperienceYears() != null && notBlank(pref.getLocation())),
                new Step("contact", "Add your phone and LinkedIn", notBlank(user.getPhone()) && notBlank(user.getLinkedinUrl())),
                new Step("search", "Find your first matching jobs", pref.getLastSyncAt() != null),
                new Step("apply", "Apply to a job and track it", all.stream().anyMatch(j -> j.getAppliedAt() != null)));
        int completion = (int) Math.round(100.0 * steps.stream().filter(Step::done).count() / steps.size());

        return new DashboardDto(user.getName(), steps, completion, all.size(), newThisWeek, byStatus, topMatches,
                closingSoon, recentApplications, top(missing, 8), top(matched, 8), averageMatch, pref.getLastSyncAt(),
                hasResume);
    }

    private static List<SkillCount> top(Map<String, Long> counts, int n) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(n)
                .map(e -> new SkillCount(e.getKey(), e.getValue()))
                .toList();
    }

    private static List<String> split(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String s : csv.split(",")) {
            if (!s.isBlank()) out.add(s.trim());
        }
        return out;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
