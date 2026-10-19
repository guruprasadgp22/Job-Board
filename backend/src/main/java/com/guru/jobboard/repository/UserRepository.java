package com.guru.jobboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guru.jobboard.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
