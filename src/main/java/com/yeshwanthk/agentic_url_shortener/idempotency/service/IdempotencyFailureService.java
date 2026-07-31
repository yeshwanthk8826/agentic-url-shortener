package com.yeshwanthk.agentic_url_shortener.idempotency.service;

import com.yeshwanthk.agentic_url_shortener.idempotency.repository.IdempotencyRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class IdempotencyFailureService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;

    public IdempotencyFailureService(
            IdempotencyRecordRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String key) {
        repository.findById(key).ifPresent(record -> {
            record.fail(clock.instant());
            repository.save(record);
        });
    }
}
