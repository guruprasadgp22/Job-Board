package com.guru.jobboard.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.entity.RemovedJob;

public interface RemovedJobRepository extends JpaRepository<RemovedJob, Long> {

    boolean existsByUserIdAndSourceAndExternalId(Long userId, String source, String externalId);

    @Transactional
    @Modifying
    @Query("delete from RemovedJob r where r.userId = :userId")
    void deleteAllForUser(Long userId);

    @Transactional
    @Modifying
    @Query("delete from RemovedJob r where r.removedAt < :before")
    int deleteOlderThan(Instant before);
}
