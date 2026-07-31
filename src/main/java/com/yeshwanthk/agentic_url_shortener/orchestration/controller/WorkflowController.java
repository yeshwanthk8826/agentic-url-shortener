package com.yeshwanthk.agentic_url_shortener.orchestration.controller;

import com.yeshwanthk.agentic_url_shortener.orchestration.dto.CompleteNodeRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.CreateWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.ReplanWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.WorkflowResponse;
import com.yeshwanthk.agentic_url_shortener.orchestration.service.WorkflowEngine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    public WorkflowController(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponse> create(
            @Valid @RequestBody CreateWorkflowRequest request
    ) {
        WorkflowResponse response = workflowEngine.create(request);

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/workflows/" + response.id()
                ))
                .body(response);
    }

    @GetMapping("/{workflowId}")
    public WorkflowResponse get(@PathVariable UUID workflowId) {
        return workflowEngine.get(workflowId);
    }

    @PostMapping("/{workflowId}/advance")
    public WorkflowResponse advance(
            @PathVariable UUID workflowId
    ) {
        return workflowEngine.advance(workflowId);
    }

    @PostMapping("/{workflowId}/nodes/{nodeKey}/complete")
    public WorkflowResponse completeNode(
            @PathVariable UUID workflowId,
            @PathVariable String nodeKey,
            @Valid @RequestBody CompleteNodeRequest request
    ) {
        return workflowEngine.completeNode(
                workflowId,
                nodeKey,
                request
        );
    }

    @PostMapping("/{workflowId}/replan")
    public WorkflowResponse replan(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ReplanWorkflowRequest request
    ) {
        return workflowEngine.replan(
                workflowId,
                request.changedNodeKey()
        );
    }
}