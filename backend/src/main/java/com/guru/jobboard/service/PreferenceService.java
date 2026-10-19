package com.guru.jobboard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.dto.PreferenceDto;
import com.guru.jobboard.entity.Preference;
import com.guru.jobboard.repository.PreferenceRepository;

@Service
public class PreferenceService {

    private final PreferenceRepository repository;

    public PreferenceService(PreferenceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Preference get(long userId) {
        return repository.findByUserId(userId).orElseGet(() -> repository.save(new Preference(userId)));
    }

    @Transactional
    public Preference update(long userId, PreferenceDto dto) {
        Preference p = get(userId);
        p.setKeywords(dto.keywords() == null ? "" : dto.keywords().trim());
        p.setExcludeKeywords(dto.excludeKeywords() == null ? "" : dto.excludeKeywords());
        p.setLocation(dto.location() == null ? "" : dto.location());
        p.setCountry(dto.country() == null || dto.country().isBlank() ? "in" : dto.country().trim().toLowerCase());
        p.setRemoteOnly(dto.remoteOnly());
        p.setMinScore(dto.minScore());
        return repository.save(p);
    }

    @Transactional
    public Preference save(Preference p) {
        return repository.save(p);
    }
}
