package com.yeshwanthk.agentic_url_shortener.unit.url.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectCache;
import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectTarget;
import com.yeshwanthk.agentic_url_shortener.url.domain.ShortUrlStatus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RedirectCacheTest {

    @Test
    void loadsValueOnlyOnceWhileCached() {
        var caffeine = Caffeine.newBuilder()
                .maximumSize(100)
                .<String, RedirectTarget>build();

        var cache = new RedirectCache(caffeine);
        var loadCount = new AtomicInteger();

        RedirectTarget first = cache.get(
                "Ab12Cd34",
                key -> {
                    loadCount.incrementAndGet();

                    return new RedirectTarget(
                            "https://example.com",
                            ShortUrlStatus.ACTIVE,
                            null
                    );
                }
        );

        RedirectTarget second = cache.get(
                "Ab12Cd34",
                key -> {
                    loadCount.incrementAndGet();

                    return new RedirectTarget(
                            "https://different.example.com",
                            ShortUrlStatus.ACTIVE,
                            null
                    );
                }
        );

        assertThat(first.originalUrl())
                .isEqualTo("https://example.com");

        assertThat(second.originalUrl())
                .isEqualTo("https://example.com");

        assertThat(loadCount).hasValue(1);
    }

    @Test
    void reloadsValueAfterEviction() {
        var caffeine = Caffeine.newBuilder()
                .maximumSize(100)
                .<String, RedirectTarget>build();

        var cache = new RedirectCache(caffeine);
        var loadCount = new AtomicInteger();

        cache.get(
                "Ab12Cd34",
                key -> {
                    loadCount.incrementAndGet();

                    return new RedirectTarget(
                            "https://example.com",
                            ShortUrlStatus.ACTIVE,
                            null
                    );
                }
        );

        cache.evict("Ab12Cd34");

        cache.get(
                "Ab12Cd34",
                key -> {
                    loadCount.incrementAndGet();

                    return new RedirectTarget(
                            "https://example.com",
                            ShortUrlStatus.ACTIVE,
                            null
                    );
                }
        );

        assertThat(loadCount).hasValue(2);
    }
}