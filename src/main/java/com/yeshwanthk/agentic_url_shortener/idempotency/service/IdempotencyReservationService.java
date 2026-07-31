package com.yeshwanthk.agentic_url_shortener.idempotency.service;

import com.yeshwanthk.agentic_url_shortener.idempotency.domain.IdempotencyRecord;
import com.yeshwanthk.agentic_url_shortener.idempotency.dto.IdempotencyReservation;
import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencyConflictException;
import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencyInProgressException;
import com.yeshwanthk.agentic_url_shortener.idempotency.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class IdempotencyReservationService {

    private static final Duration RECORD_TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyReservationService(
            IdempotencyRecordRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyReservation reserve(
            String key,
            String requestHash
    ) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(RECORD_TTL);

        int inserted = repository.reserveIfAbsent(
                key,
                requestHash,
                now,
                expiresAt
        );

        IdempotencyRecord record = repository.findById(key)
                .orElseThrow(() -> new IllegalStateException(
                        "Reserved idempotency record could not be loaded"
                ));

        if (!record.hasRequestHash(requestHash)) {
            throw new IdempotencyConflictException();
        }

        if (inserted == 1) {
            return IdempotencyReservation.acquired();
        }

        if (record.isCompleted()) {
            return IdempotencyReservation.replay(record.getResponseBody());
        }

        if (record.isFailed() || record.isExpiredAt(now)) {
            record.restart(now, expiresAt);
            repository.save(record);

            return IdempotencyReservation.acquired();
        }

        throw new IdempotencyInProgressException();
    }
}
