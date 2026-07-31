package com.yeshwanthk.agentic_url_shortener.url.exception;

public class InvalidUrlException extends RuntimeException {

    public InvalidUrlException(String message) {
        super(message);
    }
}