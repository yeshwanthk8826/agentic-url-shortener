package com.yeshwanthk.agentic_url_shortener.orchestration.controller;

import com.yeshwanthk.agentic_url_shortener.orchestration.dto.ApproveWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.AuditEventResponse;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.SafeStopWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.WorkflowResponse;
import com.yeshwanthk.agentic_url_shortener.orchestration.service.WorkflowEngine;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workflows/{workflowId}/governance")
public class WorkflowGovernanceController {

    private final WorkflowEngine workflowEngine;

    public WorkflowGovernanceController(
            WorkflowEngine workflowEngine
    ) {
        this.workflowEngine = workflowEngine;
    }

    @PostMapping("/approvals/release-readiness")
    public WorkflowResponse approveRelease(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ApproveWorkflowRequest request
    ) {
        return workflowEngine.approveRelease(
                workflowId,
                request.actor(),
                request.reason()
        );
    }

    @PostMapping("/safe-stop")
    public WorkflowResponse safeStop(
            @PathVariable UUID workflowId,
            @Valid @RequestBody SafeStopWorkflowRequest request
    ) {
        return workflowEngine.safeStop(
                workflowId,
                request.actor(),
                request.reason()
        );
    }

    @GetMapping("/audit-events")
    public List<AuditEventResponse> auditEvents(
            @PathVariable UUID workflowId
    ) {
        return workflowEngine.auditEvents(workflowId);
    }
}