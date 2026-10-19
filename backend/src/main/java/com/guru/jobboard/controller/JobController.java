package com.guru.jobboard.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guru.jobboard.dto.JobDto;
import com.guru.jobboard.dto.JobUpdateRequest;
import com.guru.jobboard.dto.ManualJobRequest;
import com.guru.jobboard.dto.PageDto;
import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.JobCleanupService;
import com.guru.jobboard.service.JobService;
import com.guru.jobboard.service.LinkChecker;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobCleanupService cleanupService;

    public JobController(JobService jobService, JobCleanupService cleanupService) {
        this.jobService = jobService;
        this.cleanupService = cleanupService;
    }

    @GetMapping
    public PageDto<JobDto> list(@AuthenticationPrincipal AuthUser me,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) List<ApplicationStatus> status,
                                @RequestParam(required = false) String source,
                                @RequestParam(required = false) Boolean remote,
                                @RequestParam(defaultValue = "0") int minScore,
                                @RequestParam(defaultValue = "score") String sort,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return jobService.search(me.id(), q, status, source, remote, minScore, sort, page, size);
    }

    @GetMapping("/{id}")
    public JobDto get(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return jobService.get(me.id(), id);
    }

    /** Paste a job you found elsewhere. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JobDto add(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ManualJobRequest request) {
        return jobService.addManual(me.id(), request);
    }

    @PatchMapping("/{id}")
    public JobDto update(@AuthenticationPrincipal AuthUser me, @PathVariable long id,
                         @RequestBody JobUpdateRequest request) {
        return jobService.update(me.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        jobService.delete(me.id(), id);
    }

    /** Checks right now whether the posting still accepts applications; deletes it if closed. */
    @PostMapping("/{id}/check")
    public Map<String, Object> check(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        LinkChecker.Result result = cleanupService.checkOne(jobService.find(me.id(), id));
        return Map.of("result", result, "deleted", result == LinkChecker.Result.CLOSED);
    }
}
