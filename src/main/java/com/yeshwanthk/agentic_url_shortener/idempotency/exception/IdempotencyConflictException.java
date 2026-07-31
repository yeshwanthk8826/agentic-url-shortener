package com.yeshwanthk.agentic_url_shortener.idempotency.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super(
                "The idempotency key was already used with a different request"
        );
    }
}
