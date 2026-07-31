package com.yeshwanthk.agentic_url_shortener.idempotency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdempotencyStatus status;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected IdempotencyRecord() {
        // Required by JPA.
    }

    public IdempotencyRecord(
            String key,
            String requestHash,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.key = Objects.requireNonNull(key);
        this.requestHash = Objects.requireNonNull(requestHash);
        this.status = IdempotencyStatus.IN_PROGRESS;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
        this.expiresAt = Objects.requireNonNull(expiresAt);
    }

    public boolean hasRequestHash(String candidateHash) {
        return requestHash.equals(candidateHash);
    }

    public boolean isCompleted() {
        return status == IdempotencyStatus.COMPLETED;
    }

    public boolean isInProgress() {
        return status == IdempotencyStatus.IN_PROGRESS;
    }

    public boolean isFailed() {
        return status == IdempotencyStatus.FAILED;
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public void complete(
            int responseStatus,
            String responseBody,
            Instant completedAt
    ) {
        if (status != IdempotencyStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Only an in-progress idempotency record can be completed"
            );
        }

        this.status = IdempotencyStatus.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = Objects.requireNonNull(responseBody);
        this.updatedAt = Objects.requireNonNull(completedAt);
    }

    public void fail(Instant failedAt) {
        if (status == IdempotencyStatus.COMPLETED) {
            return;
        }

        this.status = IdempotencyStatus.FAILED;
        this.responseStatus = null;
        this.responseBody = null;
        this.updatedAt = Objects.requireNonNull(failedAt);
    }

    public void restart(Instant restartedAt, Instant newExpiresAt) {
        if (status == IdempotencyStatus.COMPLETED) {
            throw new IllegalStateException(
                    "A completed idempotency record cannot be restarted"
            );
        }

        this.status = IdempotencyStatus.IN_PROGRESS;
        this.responseStatus = null;
        this.responseBody = null;
        this.updatedAt = Objects.requireNonNull(restartedAt);
        this.expiresAt = Objects.requireNonNull(newExpiresAt);
    }

    public String getKey() {
        return key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public long getVersion() {
        return version;
    }
}
