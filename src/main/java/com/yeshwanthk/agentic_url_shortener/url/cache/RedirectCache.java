package com.yeshwanthk.agentic_url_shortener.url.cache;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class RedirectCache {

    private final Cache<String, RedirectTarget> cache;

    public RedirectCache(Cache<String, RedirectTarget> cache) {
        this.cache = cache;
    }

    public RedirectTarget get(
            String shortCode,
            Function<String, RedirectTarget> loader
    ) {
        return cache.get(shortCode, loader);
    }

    public void put(String shortCode, RedirectTarget target) {
        cache.put(shortCode, target);
    }

    public void evict(String shortCode) {
        cache.invalidate(shortCode);
    }
}
