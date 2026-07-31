package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        UUID workflowId,
        String eventType,
        String actor,
        String details,
        Instant createdAt
) {
}