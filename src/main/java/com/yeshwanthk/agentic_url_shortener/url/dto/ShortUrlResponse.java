package com.yeshwanthk.agentic_url_shortener.url.dto;

import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus;

import java.time.Instant;
import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String shortCode,
        String shortUrl,
        String originalUrl,
        ShortUrlStatus status,
        Instant createdAt,
        Instant expiresAt
) {

    public static ShortUrlResponse from(ShortUrl shortUrl, String publicBaseUrl) {
        return new ShortUrlResponse(
                shortUrl.getId(),
                shortUrl.getShortCode(),
                publicBaseUrl + "/" + shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getCreatedAt(),
                shortUrl.getExpiresAt()
        );
    }
}