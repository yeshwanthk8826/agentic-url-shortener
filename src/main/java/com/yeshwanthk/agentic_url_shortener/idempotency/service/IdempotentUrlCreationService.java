package com.yeshwanthk.agentic_url_shortener.idempotency.service;

import com.yeshwanthk.agentic_url_shortener.idempotency.dto.IdempotentResult;
import com.yeshwanthk.agentic_url_shortener.idempotency.dto.IdempotencyReservation;
import com.yeshwanthk.agentic_url_shortener.idempotency.exception.IdempotencySerializationException;
import com.yeshwanthk.agentic_url_shortener.idempotency.repository.IdempotencyRecordRepository;
import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.dto.ShortUrlResponse;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

@Service
public class IdempotentUrlCreationService {

    private final ShortUrlService shortUrlService;
    private final IdempotencyReservationService reservationService;
    private final IdempotencyFailureService failureService;
    private final IdempotencyRecordRepository repository;
    private final RequestFingerprint requestFingerprint;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public IdempotentUrlCreationService(
            ShortUrlService shortUrlService,
            IdempotencyReservationService reservationService,
            IdempotencyFailureService failureService,
            IdempotencyRecordRepository repository,
            RequestFingerprint requestFingerprint,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.shortUrlService = shortUrlService;
        this.reservationService = reservationService;
        this.failureService = failureService;
        this.repository = repository;
        this.requestFingerprint = requestFingerprint;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public IdempotentResult<ShortUrlResponse> create(
            String idempotencyKey,
            CreateShortUrlRequest request
    ) {
        String requestHash = requestFingerprint.create(request);

        IdempotencyReservation reservation =
                reservationService.reserve(idempotencyKey, requestHash);

        if (reservation.replay()) {
            return IdempotentResult.replayed(
                    deserialize(reservation.responseBody())
            );
        }

        try {
            ShortUrlResponse response = shortUrlService.create(request);
            String responseBody = serialize(response);

            var record = repository.findById(idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency record disappeared during processing"
                    ));

            if (!record.hasRequestHash(requestHash)) {
                throw new IllegalStateException(
                        "Idempotency request hash changed during processing"
                );
            }

            record.complete(
                    HttpStatus.CREATED.value(),
                    responseBody,
                    clock.instant()
            );

            repository.save(record);

            return IdempotentResult.created(response);
        } catch (RuntimeException exception) {
            failureService.markFailed(idempotencyKey);
            throw exception;
        }
    }

    private String serialize(ShortUrlResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JacksonException exception) {
            throw new IdempotencySerializationException(
                    "Unable to serialize the idempotent response",
                    exception
            );
        }
    }

    private ShortUrlResponse deserialize(String responseBody) {
        try {
            return objectMapper.readValue(
                    responseBody,
                    ShortUrlResponse.class
            );
        } catch (JacksonException exception) {
            throw new IdempotencySerializationException(
                    "Unable to deserialize the stored idempotent response",
                    exception
            );
        }
    }
}