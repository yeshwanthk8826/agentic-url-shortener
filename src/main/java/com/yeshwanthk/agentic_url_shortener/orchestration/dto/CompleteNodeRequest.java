package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteNodeRequest(
        @NotBlank
        @Size(max = 50000)
        String output
) {
}