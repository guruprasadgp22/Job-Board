package com.guru.jobboard.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guru.jobboard.dto.ChangePasswordRequest;
import com.guru.jobboard.dto.DashboardDto;
import com.guru.jobboard.dto.ProfileDto;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.DashboardService;
import com.guru.jobboard.service.ProfileService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final ProfileService profileService;
    private final DashboardService dashboardService;

    public ProfileController(ProfileService profileService, DashboardService dashboardService) {
        this.profileService = profileService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardDto dashboard(@AuthenticationPrincipal AuthUser me) {
        return dashboardService.build(me.id());
    }

    @GetMapping("/profile")
    public ProfileDto profile(@AuthenticationPrincipal AuthUser me) {
        return profileService.get(me.id());
    }

    @PutMapping("/profile")
    public ProfileDto update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ProfileDto dto) {
        return profileService.update(me.id(), dto);
    }

    @PutMapping("/profile/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ChangePasswordRequest req) {
        profileService.changePassword(me.id(), req);
    }

    /** POST with the password in the body, so it is not put in a URL. */
    @PostMapping("/profile/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthUser me, @RequestBody Map<String, String> body) {
        profileService.deleteAccount(me.id(), body.get("password"));
    }
}
