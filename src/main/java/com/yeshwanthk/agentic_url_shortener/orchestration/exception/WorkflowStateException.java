package com.yeshwanthk.agentic_url_shortener.orchestration.exception;

public class WorkflowStateException extends RuntimeException {

    public WorkflowStateException(String message) {
        super(message);
    }
}