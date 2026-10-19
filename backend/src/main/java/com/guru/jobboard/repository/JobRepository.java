package com.guru.jobboard.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.entity.ApplicationStatus;
import com.guru.jobboard.entity.Job;

public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdAndUserId(Long id, Long userId);

    Optional<Job> findByUserIdAndSourceAndExternalId(Long userId, String source, String externalId);

    List<Job> findByUserId(Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, ApplicationStatus status);

    @Query("select j.source, count(j) from Job j where j.userId = :userId group by j.source")
    List<Object[]> countBySource(Long userId);

    @Modifying
    @Transactional
    @Query("delete from Job j where j.userId = :userId")
    void deleteAllForUser(Long userId);

    // Cleanup runs across all users:

    List<Job> findByStatusIn(Collection<ApplicationStatus> statuses);

    List<Job> findByExpiresAtBefore(Instant now);

    List<Job> findBySourceAndStatusInAndLastSeenAtBefore(String source, Collection<ApplicationStatus> statuses, Instant before);

    @Query("select j from Job j order by j.lastCheckedAt asc nulls first")
    List<Job> findLeastRecentlyChecked(Pageable pageable);
}
