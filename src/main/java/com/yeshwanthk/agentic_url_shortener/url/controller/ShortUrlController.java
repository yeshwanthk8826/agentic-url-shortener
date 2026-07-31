package com.yeshwanthk.agentic_url_shortener.url.controller;

import com.yeshwanthk.agentic_url_shortener.idempotency.dto.IdempotentResult;
import com.yeshwanthk.agentic_url_shortener.idempotency.service.IdempotentUrlCreationService;
import com.yeshwanthk.agentic_url_shortener.url.dto.CreateShortUrlRequest;
import com.yeshwanthk.agentic_url_shortener.url.dto.ShortUrlResponse;
import com.yeshwanthk.agentic_url_shortener.url.service.ShortUrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Validated
@RestController
@RequestMapping
public class ShortUrlController {

    private static final String SHORT_CODE_PATTERN = "^[0-9A-Za-z]{8}$";

    private final ShortUrlService shortUrlService;

    private final IdempotentUrlCreationService idempotentUrlCreationService;

    public ShortUrlController(ShortUrlService shortUrlService, IdempotentUrlCreationService idempotentUrlCreationService) {
        this.shortUrlService = shortUrlService;
        this.idempotentUrlCreationService = idempotentUrlCreationService;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortUrlResponse> create(
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key is required")
            @Size(
                    max = 128,
                    message = "Idempotency-Key must not exceed 128 characters"
            )
            String idempotencyKey,

            @Valid @RequestBody CreateShortUrlRequest request
    ) {
        IdempotentResult<ShortUrlResponse> result =
                idempotentUrlCreationService.create(
                        idempotencyKey.trim(),
                        request
                );

        return ResponseEntity
                .status(result.replayed()
                        ? HttpStatus.OK
                        : HttpStatus.CREATED)
                .location(URI.create(result.response().shortUrl()))
                .header(
                        "Idempotency-Replayed",
                        Boolean.toString(result.replayed())
                )
                .body(result.response());
    }

    @GetMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<ShortUrlResponse> get(
            @PathVariable
            @Pattern(
                    regexp = SHORT_CODE_PATTERN,
                    message = "Short code must contain exactly 8 Base62 characters"
            )
            String shortCode
    ) {
        return ResponseEntity.ok(shortUrlService.findByShortCode(shortCode));
    }

    @GetMapping("/{shortCode:" + SHORT_CODE_PATTERN + "}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        URI destination = shortUrlService.resolve(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(destination)
                .build();
    }
}
