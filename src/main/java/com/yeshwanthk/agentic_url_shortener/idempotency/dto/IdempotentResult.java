package com.yeshwanthk.agentic_url_shortener.idempotency.dto;

public record IdempotentResult<T>(
        T response,
        boolean replayed
) {
    public static <T> IdempotentResult<T> created(T response) {
        return new IdempotentResult<>(response, false);
    }

    public static <T> IdempotentResult<T> replayed(T response) {
        return new IdempotentResult<>(response, true);
    }
}