package com.guru.jobboard.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guru.jobboard.config.AppProperties;
import com.guru.jobboard.dto.AuthResponse;
import com.guru.jobboard.dto.LoginRequest;
import com.guru.jobboard.dto.RegisterRequest;
import com.guru.jobboard.dto.UserDto;
import com.guru.jobboard.entity.Role;
import com.guru.jobboard.entity.User;
import com.guru.jobboard.repository.UserRepository;
import com.guru.jobboard.security.JwtService;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PreferenceService preferences;
    private final AppProperties props;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, PreferenceService preferences,
                       AppProperties props) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.preferences = preferences;
        this.props = props;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (!props.allowRegistration()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "New sign-ups are turned off");
        }
        String email = normalize(req.email());
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        Role role = req.role() == null ? Role.SEEKER : req.role();
        String organization = req.organization() == null ? "" : req.organization().trim();
        if (role == Role.EMPLOYER && organization.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter your organization's name");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(role);
        user.setOrganization(role == Role.EMPLOYER ? organization : null);
        users.save(user);
        if (role == Role.SEEKER) {
            preferences.get(user.getId());
        }
        return new AuthResponse(jwt.createToken(user), UserDto.from(user));
    }

    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmail(normalize(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wrong email or password"));
        return new AuthResponse(jwt.createToken(user), UserDto.from(user));
    }

    public UserDto me(long userId) {
        return users.findById(userId).map(UserDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
