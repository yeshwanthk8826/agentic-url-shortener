package com.yeshwanthk.agentic_url_shortener.url.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortUrlRequest(

        @NotBlank(message = "URL is required")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String url,

        @Future(message = "Expiration must be in the future")
        Instant expiresAt
) {
}