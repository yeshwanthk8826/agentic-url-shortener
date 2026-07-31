package com.yeshwanthk.agentic_url_shortener.idempotency.repository;

import com.yeshwanthk.agentic_url_shortener.idempotency.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface IdempotencyRecordRepository
        extends JpaRepository<IdempotencyRecord, String> {

    /**
     * PostgreSQL ON CONFLICT makes reservation atomic across application
     * instances. A return value of 1 means this request owns the reservation.
     * A return value of 0 means the key already existed.
     */
    @Modifying
    @Query(
            value = """
                    INSERT INTO idempotency_records (
                        idempotency_key,
                        request_hash,
                        status,
                        created_at,
                        updated_at,
                        expires_at,
                        version
                    )
                    VALUES (
                        :key,
                        :requestHash,
                        'IN_PROGRESS',
                        :createdAt,
                        :createdAt,
                        :expiresAt,
                        0
                    )
                    ON CONFLICT (idempotency_key) DO NOTHING
                    """,
            nativeQuery = true
    )
    int reserveIfAbsent(
            @Param("key") String key,
            @Param("requestHash") String requestHash,
            @Param("createdAt") Instant createdAt,
            @Param("expiresAt") Instant expiresAt
    );
}
