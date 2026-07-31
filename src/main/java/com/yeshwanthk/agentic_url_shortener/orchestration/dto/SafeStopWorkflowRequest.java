package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SafeStopWorkflowRequest(
        @NotBlank
        @Size(max = 150)
        String actor,

        @NotBlank
        @Size(max = 2000)
        String reason
) {
}