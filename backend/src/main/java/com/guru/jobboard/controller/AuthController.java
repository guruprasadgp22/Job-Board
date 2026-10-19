package com.guru.jobboard.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.dto.AuthResponse;
import com.guru.jobboard.dto.LoginRequest;
import com.guru.jobboard.dto.RegisterRequest;
import com.guru.jobboard.dto.UserDto;
import com.guru.jobboard.security.AuthUser;
import com.guru.jobboard.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AppProperties props;

    public AuthController(AuthService authService, AppProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDto me(@AuthenticationPrincipal AuthUser me) {
        return authService.me(me.id());
    }

    /** Lets the login page hide the "Create account" option when sign-ups are off. */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of("allowRegistration", props.allowRegistration());
    }
}
