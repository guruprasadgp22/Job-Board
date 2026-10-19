package com.guru.jobboard.dto;

import com.guru.jobboard.entity.User;

public record UserDto(Long id, String name, String email) {

    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getName(), u.getEmail());
    }
}
