package com.guru.jobboard.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** A job an employer posted on this site. Job seekers apply to it here (see {@link JobApplication}). */
@Entity
@Table(name = "postings", indexes = {
        @Index(columnList = "employer_id"),
        @Index(columnList = "is_open")
})
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    /** Copied from the employer's profile; kept in sync when they rename it. */
    @Column(nullable = false, length = 150)
    private String organization;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 200)
    private String location;

    private boolean remote;

    /** Full-time, Part-time, Contract, Internship. */
    @Column(name = "employment_type", length = 30)
    private String employmentType;

    @Column(name = "min_experience")
    private Integer minExperience;

    @Column(name = "max_experience")
    private Integer maxExperience;

    @Column(length = 100)
    private String salary;

    /** Comma-separated skills the job needs; used to score applicants and shown to seekers. */
    @Column(length = 1000)
    private String skills;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Last day to apply, if any. Applications are refused after it. */
    private Instant deadline;

    /** Closed postings are hidden from job seekers and stop taking applications. */
    @Column(name = "is_open", nullable = false)
    private boolean open = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public boolean acceptsApplications() {
        return open && (deadline == null || deadline.isAfter(Instant.now()));
    }

    public Long getId() { return id; }
    public Long getEmployerId() { return employerId; }
    public void setEmployerId(Long employerId) { this.employerId = employerId; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean remote) { this.remote = remote; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public Integer getMinExperience() { return minExperience; }
    public void setMinExperience(Integer minExperience) { this.minExperience = minExperience; }
    public Integer getMaxExperience() { return maxExperience; }
    public void setMaxExperience(Integer maxExperience) { this.maxExperience = maxExperience; }
    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
