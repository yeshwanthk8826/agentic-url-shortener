package com.yeshwanthk.agentic_url_shortener.orchestration.exception;

import java.util.UUID;

public class WorkflowNotFoundException extends RuntimeException {

    public WorkflowNotFoundException(UUID id) {
        super("Workflow not found: " + id);
    }
}