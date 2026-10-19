package com.guru.jobboard.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guru.jobboard.dto.ApplyRequest;
import com.guru.jobboard.dto.MyApplicationDto;
import com.guru.jobboard.dto.PageDto;
import com.guru.jobboard.dto.PostingDto;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.ApplicationService;
import com.guru.jobboard.service.PostingService;

import jakarta.validation.Valid;

/** Job seekers only (see SecurityConfig): browse jobs employers posted here and apply to them. */
@RestController
public class PostingController {

    private final PostingService postingService;
    private final ApplicationService applicationService;

    public PostingController(PostingService postingService, ApplicationService applicationService) {
        this.postingService = postingService;
        this.applicationService = applicationService;
    }

    @GetMapping("/api/postings")
    public PageDto<PostingDto> browse(@AuthenticationPrincipal AuthUser me,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) Boolean remote,
                                      @RequestParam(defaultValue = "match") String sort,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return postingService.browse(me.id(), q, remote, sort, page, size);
    }

    @GetMapping("/api/postings/{id}")
    public PostingDto get(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        return postingService.getForSeeker(me.id(), id);
    }

    @PostMapping("/api/postings/{id}/apply")
    @ResponseStatus(HttpStatus.CREATED)
    public MyApplicationDto apply(@AuthenticationPrincipal AuthUser me, @PathVariable long id,
                                  @Valid @RequestBody(required = false) ApplyRequest req) {
        return applicationService.apply(me.id(), id, req == null ? null : req.coverNote());
    }

    @DeleteMapping("/api/postings/{id}/apply")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(@AuthenticationPrincipal AuthUser me, @PathVariable long id) {
        applicationService.withdraw(me.id(), id);
    }

    @GetMapping("/api/applications")
    public List<MyApplicationDto> mine(@AuthenticationPrincipal AuthUser me) {
        return applicationService.mine(me.id());
    }
}
