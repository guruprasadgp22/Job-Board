package com.guru.jobboard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.entity.Posting;

public interface PostingRepository extends JpaRepository<Posting, Long>, JpaSpecificationExecutor<Posting> {

    List<Posting> findByEmployerIdOrderByCreatedAtDesc(Long employerId);

    Optional<Posting> findByIdAndEmployerId(Long id, Long employerId);

    @Modifying
    @Transactional
    @Query("update Posting p set p.organization = :organization where p.employerId = :employerId")
    void renameOrganization(Long employerId, String organization);
}
