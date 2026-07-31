package com.yeshwanthk.agentic_url_shortener.url.repository;

import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    /**
     * Atomically records a successful redirect without loading and locking
     * the complete entity.
     *
     * The expiration and status predicates protect against a cached target
     * becoming unavailable immediately before the update.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ShortUrl shortUrl
               SET shortUrl.visitCount = shortUrl.visitCount + 1,
                   shortUrl.lastAccessedAt = :accessedAt,
                   shortUrl.updatedAt = :accessedAt
             WHERE shortUrl.shortCode = :shortCode
               AND shortUrl.status =
                   com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus.ACTIVE
               AND (
                   shortUrl.expiresAt IS NULL
                   OR shortUrl.expiresAt > :accessedAt
               )
            """)
    int recordVisit(
            @Param("shortCode") String shortCode,
            @Param("accessedAt") Instant accessedAt
    );
}
