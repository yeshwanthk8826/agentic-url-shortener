package com.yeshwanthk.agentic_url_shortener.url.cache;

import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrl;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus;

import java.time.Instant;

public record RedirectTarget(
        String originalUrl,
        ShortUrlStatus status,
        Instant expiresAt
) {

    public static RedirectTarget from(ShortUrl shortUrl) {
        return new RedirectTarget(
                shortUrl.getOriginalUrl(),
                shortUrl.getStatus(),
                shortUrl.getExpiresAt()
        );
    }

    public boolean isResolvableAt(Instant instant) {
        return status == ShortUrlStatus.ACTIVE
                && (expiresAt == null || expiresAt.isAfter(instant));
    }
}
