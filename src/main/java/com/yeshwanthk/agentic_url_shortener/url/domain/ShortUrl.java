package com.yeshwanthk.agentic_url_shortener.url.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 16)
    private String shortCode;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShortUrlStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;

    protected ShortUrl() {
        // Required by JPA.
    }

    private ShortUrl(
            UUID id,
            String shortCode,
            String originalUrl,
            String normalizedUrl,
            ShortUrlStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant expiresAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.shortCode = Objects.requireNonNull(shortCode);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.normalizedUrl = Objects.requireNonNull(normalizedUrl);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.expiresAt = expiresAt;
    }

    public static ShortUrl create(
            String shortCode,
            String originalUrl,
            String normalizedUrl,
            Instant createdAt,
            Instant expiresAt
    ) {
        if (expiresAt != null && !expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("Expiration must be after creation time");
        }

        return new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                originalUrl,
                normalizedUrl,
                ShortUrlStatus.ACTIVE,
                createdAt,
                createdAt,
                expiresAt
        );
    }

    public boolean isResolvableAt(Instant instant) {
        return status == ShortUrlStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(instant));
    }

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getNormalizedUrl() {
        return normalizedUrl;
    }

    public ShortUrlStatus getStatus() {
        return status;
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

    public long getVisitCount() {
        return visitCount;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
}