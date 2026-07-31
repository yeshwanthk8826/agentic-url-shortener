package com.yeshwanthk.agentic_url_shortener.idempotency.exception;

public class IdempotencyInProgressException extends RuntimeException {

    public IdempotencyInProgressException() {
        super("A request with this idempotency key is already in progress");
    }
}
