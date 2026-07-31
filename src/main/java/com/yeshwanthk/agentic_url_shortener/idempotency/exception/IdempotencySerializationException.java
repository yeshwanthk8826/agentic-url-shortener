package com.yeshwanthk.agentic_url_shortener.idempotency.exception;

public class IdempotencySerializationException extends RuntimeException {

    public IdempotencySerializationException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
