package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import jakarta.validation.constraints.NotBlank;

public record ReplanWorkflowRequest(
        @NotBlank String changedNodeKey
) {
}