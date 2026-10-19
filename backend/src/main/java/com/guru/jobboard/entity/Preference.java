package com.guru.jobboard.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per user: what jobs to look for, plus their resume and the skills extracted from it. */
@Entity
@Table(name = "preferences")
public class Preference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** The job title you want, e.g. "Java Developer". Used as the main search and to boost matching titles. */
    @Column(name = "desired_role", length = 150)
    private String desiredRole;

    /** Years of experience; used to down-rank jobs far above or below your level. */
    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    /** Comma-separated, e.g. "java, spring boot, react". The first few are used as search terms. */
    @Column(nullable = false, length = 1000)
    private String keywords = "";

    /** Words that disqualify a job if they appear in the title, e.g. "senior, lead, php". */
    @Column(length = 1000)
    private String excludeKeywords = "";

    /** Free-text location used by Adzuna and for scoring, e.g. "Bangalore". */
    @Column(length = 200)
    private String location = "";

    /** Adzuna country code: in, gb, us, de, ... */
    @Column(length = 5)
    private String country = "in";

    private boolean remoteOnly = false;

    /** Jobs scoring below this (0-100) are not stored. */
    private int minScore = 30;

    @Column(name = "resume_text", columnDefinition = "TEXT")
    private String resumeText;

    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    /** Comma-separated skills from the resume (editable), most mentioned first. */
    @Column(name = "resume_skills", length = 2000)
    private String resumeSkills;

    @Column(name = "resume_updated_at")
    private Instant resumeUpdatedAt;

    protected Preference() {
    }

    public Preference(Long userId) {
        this.userId = userId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getDesiredRole() { return desiredRole; }
    public void setDesiredRole(String desiredRole) { this.desiredRole = desiredRole; }
    public Integer getExperienceYears() { return experienceYears; }
    public void setExperienceYears(Integer experienceYears) { this.experienceYears = experienceYears; }
    public Instant getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(Instant lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(String excludeKeywords) { this.excludeKeywords = excludeKeywords; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public boolean isRemoteOnly() { return remoteOnly; }
    public void setRemoteOnly(boolean remoteOnly) { this.remoteOnly = remoteOnly; }
    public int getMinScore() { return minScore; }
    public void setMinScore(int minScore) { this.minScore = minScore; }
    public String getResumeText() { return resumeText; }
    public void setResumeText(String resumeText) { this.resumeText = resumeText; }
    public String getResumeFileName() { return resumeFileName; }
    public void setResumeFileName(String resumeFileName) { this.resumeFileName = resumeFileName; }
    public String getResumeSkills() { return resumeSkills; }
    public void setResumeSkills(String resumeSkills) { this.resumeSkills = resumeSkills; }
    public Instant getResumeUpdatedAt() { return resumeUpdatedAt; }
    public void setResumeUpdatedAt(Instant resumeUpdatedAt) { this.resumeUpdatedAt = resumeUpdatedAt; }
}
