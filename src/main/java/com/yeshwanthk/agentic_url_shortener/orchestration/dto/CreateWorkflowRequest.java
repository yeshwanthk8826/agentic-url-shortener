package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import com.yeshwanthk.agentic_url_shortener.orchestration.domain.ScenarioType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWorkflowRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 10000)
        String requirement,

        @NotNull
        ScenarioType scenarioType
) {
}