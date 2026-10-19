package com.guru.jobboard.dto;

import com.guru.jobboard.entity.Role;
import com.guru.jobboard.entity.User;

public record UserDto(Long id, String name, String email, Role role, String organization) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getOrganization());
    }
}
