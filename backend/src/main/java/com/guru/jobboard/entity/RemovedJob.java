package com.guru.jobboard.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Remembers deleted jobs so the next sync does not add them again. Pruned after a while. */
@Entity
@Table(name = "removed_jobs", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "source", "external_id"}))
public class RemovedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String source;

    @Column(name = "external_id", nullable = false, length = 300)
    private String externalId;

    @Column(length = 200)
    private String reason;

    @Column(name = "removed_at", nullable = false)
    private Instant removedAt;

    protected RemovedJob() {
    }

    public RemovedJob(Long userId, String source, String externalId, String reason) {
        this.userId = userId;
        this.source = source;
        this.externalId = externalId;
        this.reason = reason;
        this.removedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getReason() { return reason; }
    public Instant getRemovedAt() { return removedAt; }
}
