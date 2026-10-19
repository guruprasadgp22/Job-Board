package com.guru.jobboard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guru.jobboard.entity.Preference;

public interface PreferenceRepository extends JpaRepository<Preference, Long> {

    Optional<Preference> findByUserId(Long userId);
}
