package com.yeshwanthk.agentic_url_shortener.idempotency.dto;

public record IdempotencyReservation(
        boolean replay,
        String responseBody
) {
    public static IdempotencyReservation acquired() {
        return new IdempotencyReservation(false, null);
    }

    public static IdempotencyReservation replay(String responseBody) {
        return new IdempotencyReservation(true, responseBody);
    }
}
