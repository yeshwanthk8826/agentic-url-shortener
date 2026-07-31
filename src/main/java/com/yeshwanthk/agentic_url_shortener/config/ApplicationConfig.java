package com.yeshwanthk.agentic_url_shortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationConfig {

    /**
     * Injecting Clock makes expiration behavior deterministic in unit tests.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}