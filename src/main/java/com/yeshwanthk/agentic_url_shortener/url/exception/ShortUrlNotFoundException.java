package com.yeshwanthk.agentic_url_shortener.url.exception;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String shortCode) {
        super("Short URL was not found or is no longer available: " + shortCode);
    }
}