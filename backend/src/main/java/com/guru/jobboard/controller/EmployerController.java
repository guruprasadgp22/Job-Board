package com.guru.jobboard.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guru.jobboard.dto.ApplicantDto;
import com.guru.jobboard.dto.ApplicantUpdateRequest;
import com.guru.jobboard.dto.EmployerDashboardDto;
import com.guru.jobboard.dto.PostingDto;
import com.guru.jobboard.dto.PostingRequest;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.ApplicationService;
import com.guru.jobboard.service.EmployerDashboardService;
import com.guru.jobboard.service.PostingService;

import jakarta.validation.Valid;

/** Employers only (see SecurityConfig): post jobs for their organization and review applicants. */
@RestController
@RequestMapping("/api/employer")
public class EmployerController {

    private final PostingService postingService;
    private final ApplicationService applicationService;
    private final EmployerDashboardService dashboardService;

    public EmployerController(PostingService postingService, ApplicationService applicationService,
                              EmployerDashboardService dashboardService) {
        this.postingService = postingService;
        this.applicationService = applicationService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public EmployerDashboardDto dashboard(@AuthenticationPrincipal AuthUser me) {
        return dashboardService.build(me.id());
    }

    @GetMapping("/postings")
    public List<PostingDto> postings(@AuthenticationPrincipal AuthUser me) {
        return postingService.listForEmployer(me.id());
    }

    @GetMapping("/postings/{id}")
    public PostingDto posting(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return postingService.getForEmployer(me.id(), id);
    }

    @PostMapping("/postings")
    @ResponseStatus(HttpStatus.CREATED)
    public PostingDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody PostingRequest req) {
        return postingService.create(me.id(), req);
    }

    @PutMapping("/postings/{id}")
    public PostingDto update(@AuthenticationPrincipal AuthUser me, @PathVariable long id,
                             @Valid @RequestBody PostingRequest req) {
        return postingService.update(me.id(), id, req);
    }

    @PostMapping("/postings/{id}/close")
    public PostingDto close(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return postingService.setOpen(me.id(), id, false);
    }

    @PostMapping("/postings/{id}/reopen")
    public PostingDto reopen(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return postingService.setOpen(me.id(), id, true);
    }

    @DeleteMapping("/postings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        postingService.delete(me.id(), id);
    }

    @GetMapping("/postings/{id}/applicants")
    public List<ApplicantDto> applicants(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return applicationService.forPosting(me.id(), id);
    }

    @GetMapping("/applicants")
    public List<ApplicantDto> allApplicants(@AuthenticationPrincipal AuthUser me) {
        return applicationService.forEmployer(me.id());
    }

    @GetMapping("/applicants/{id}")
    public ApplicantDto applicant(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return applicationService.get(me.id(), id);
    }

    @PatchMapping("/applicants/{id}")
    public ApplicantDto updateApplicant(@AuthenticationPrincipal AuthUser me, @PathVariable long id,
                                        @Valid @RequestBody ApplicantUpdateRequest req) {
        return applicationService.update(me.id(), id, req);
    }
}
