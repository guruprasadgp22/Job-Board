package com.guru.jobboard.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.dto.CleanupResult;
import com.guru.jobboard.dto.PreferenceDto;
import com.guru.jobboard.dto.StatsDto;
import com.guru.jobboard.dto.SyncResult;
import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.repository.JobRepository;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.JobCleanupService;
import com.guru.jobboard.service.JobSyncService;
import com.guru.jobboard.service.PreferenceService;
import com.guru.jobboard.source.JobSource;

import jakarta.validation.Valid;

/** Preferences, "find jobs now", cleanup and stats for the logged-in user. */
@RestController
@RequestMapping("/api")
public class BoardController {

    private final JobSyncService syncService;
    private final JobCleanupService cleanupService;
    private final PreferenceService preferenceService;
    private final JobRepository jobs;
    private final List<JobSource> sources;
    private final AppProperties props;

    public BoardController(JobSyncService syncService, JobCleanupService cleanupService,
                           PreferenceService preferenceService, JobRepository jobs, List<JobSource> sources,
                           AppProperties props) {
        this.syncService = syncService;
        this.cleanupService = cleanupService;
        this.preferenceService = preferenceService;
        this.jobs = jobs;
        this.sources = sources;
        this.props = props;
    }

    @PostMapping("/sync")
    public SyncResult sync(@AuthenticationPrincipal AuthUser me) {
        return syncService.syncUser(me.id());
    }

    @PostMapping("/cleanup")
    public CleanupResult cleanup() {
        return cleanupService.cleanup();
    }

    /** For the external cron (GitHub Actions): sync every user, then clean up. Needs the X-Cron-Secret header. */
    @PostMapping("/refresh")
    public Map<String, Object> refresh(@RequestHeader(value = "X-Cron-Secret", required = false) String secret) {
        String expected = props.cronSecret();
        if (expected == null || expected.isBlank() || secret == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid cron secret");
        }
        Map<Long, SyncResult> sync = syncService.syncAll();
        CleanupResult cleanup = cleanupService.cleanup();
        int added = sync.values().stream().mapToInt(SyncResult::added).sum();
        return Map.of("users", sync.size(), "added", added, "cleanup", cleanup);
    }

    @GetMapping("/preferences")
    public PreferenceDto preferences(@AuthenticationPrincipal AuthUser me) {
        return PreferenceDto.from(preferenceService.get(me.id()));
    }

    @PutMapping("/preferences")
    public PreferenceDto updatePreferences(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PreferenceDto dto) {
        PreferenceDto saved = PreferenceDto.from(preferenceService.update(me.id(), dto));
        syncService.rescore(me.id());
        return saved;
    }

    @GetMapping("/stats")
    public StatsDto stats(@AuthenticationPrincipal AuthUser me) {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        Arrays.stream(ApplicationStatus.values())
                .forEach(s -> byStatus.put(s.name(), jobs.countByUserIdAndStatus(me.id(), s)));
        Map<String, Long> bySource = new TreeMap<>();
        jobs.countBySource(me.id()).forEach(row -> bySource.put((String) row[0], (Long) row[1]));
        List<String> enabled = sources.stream().filter(JobSource::enabled).map(JobSource::name).toList();
        return new StatsDto(jobs.countByUserId(me.id()), byStatus, bySource, new TreeMap<>(syncService.lastSuccess()),
                enabled, props.adzuna() != null && props.adzuna().enabled(),
                props.jsearch() != null && props.jsearch().enabled());
    }
}
