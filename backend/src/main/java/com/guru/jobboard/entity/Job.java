package com.guru.jobboard.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A job posting plus your application state for it. Deleting the row deletes the application too. */
@Entity
@Table(name = "jobs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "source", "external_id"}),
        indexes = {
                @Index(columnList = "user_id"),
                @Index(columnList = "status"),
                @Index(columnList = "relevance_score"),
                @Index(columnList = "posted_at")
        })
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owner. Every user has their own copy of a job, with their own score and application status. */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "external_id", nullable = false, length = 300)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 300)
    private String company;

    @Column(length = 300)
    private String location;

    private boolean remote;

    @Column(nullable = false, length = 2000)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 1000)
    private String tags;

    @Column(length = 200)
    private String salary;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    /** Application deadline, when known (JSearch, or the page's JobPosting data). Cleanup deletes the job after it. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** True when you pasted/clipped the job yourself rather than it being found by a sync. */
    @Column(name = "added_manually")
    private Boolean addedManually;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "relevance_score")
    private int relevanceScore;

    /** Skills the job asks for that are on your resume / missing from it (comma-separated). */
    @Column(name = "matched_skills", length = 1000)
    private String matchedSkills;

    @Column(name = "missing_skills", length = 1000)
    private String missingSkills;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "applied_at")
    private Instant appliedAt;

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public boolean isAddedManually() { return Boolean.TRUE.equals(addedManually); }
    public void setAddedManually(boolean addedManually) { this.addedManually = addedManually; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(String matchedSkills) { this.matchedSkills = matchedSkills; }
    public String getMissingSkills() { return missingSkills; }
    public void setMissingSkills(String missingSkills) { this.missingSkills = missingSkills; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean remote) { this.remote = remote; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public Instant getFirstSeenAt() { return firstSeenAt; }
    public void setFirstSeenAt(Instant firstSeenAt) { this.firstSeenAt = firstSeenAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }
    public int getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(int relevanceScore) { this.relevanceScore = relevanceScore; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getAppliedAt() { return appliedAt; }
    public void setAppliedAt(Instant appliedAt) { this.appliedAt = appliedAt; }
}
