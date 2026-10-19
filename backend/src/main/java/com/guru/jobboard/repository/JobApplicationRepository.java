package com.guru.jobboard.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.guru.jobboard.entity.JobApplication;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByPostingIdAndSeekerId(Long postingId, Long seekerId);

    List<JobApplication> findBySeekerIdOrderByAppliedAtDesc(Long seekerId);

    List<JobApplication> findByPostingIdOrderByMatchScoreDescAppliedAtAsc(Long postingId);

    List<JobApplication> findByPostingIdIn(Collection<Long> postingIds);

    List<JobApplication> findBySeekerIdAndPostingIdIn(Long seekerId, Collection<Long> postingIds);

    @Modifying
    @Transactional
    @Query("delete from JobApplication a where a.postingId = :postingId")
    void deleteAllForPosting(Long postingId);

    @Modifying
    @Transactional
    @Query("delete from JobApplication a where a.postingId in (select p.id from Posting p where p.employerId = :employerId)")
    void deleteAllForEmployer(Long employerId);

    @Modifying
    @Transactional
    @Query("delete from JobApplication a where a.seekerId = :seekerId")
    void deleteAllForSeeker(Long seekerId);
}
