package com.yeshwanthk.agentic_url_shortener.url.dto;

import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus;

import java.time.Instant;

public record UrlAnalyticsResponse(
        String shortCode,
        ShortUrlStatus status,
        long visitCount,
        Instant createdAt,
        Instant lastAccessedAt,
        Instant expiresAt
) {

    public static UrlAnalyticsResponse from(ShortUrl shortUrl) {
        return new UrlAnalyticsResponse(
                shortUrl.getShortCode(),
                shortUrl.getStatus(),
                shortUrl.getVisitCount(),
                shortUrl.getCreatedAt(),
                shortUrl.getLastAccessedAt(),
                shortUrl.getExpiresAt()
        );
    }
}
