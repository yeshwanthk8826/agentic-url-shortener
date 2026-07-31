package com.yeshwanthk.agentic_url_shortener.config;

import com.yeshwanthk.agentic_url_shortener.url.cache.RedirectTarget;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class ApplicationConfig {

    /**
     * Injecting Clock makes expiration behavior deterministic in unit tests.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Cache<String, RedirectTarget> redirectTargetCache(
            @Value("${app.cache.redirect.maximum-size:10000}")
            long maximumSize,

            @Value("${app.cache.redirect.ttl:10m}")
            Duration ttl
    ) {
        return Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build();
    }
}