package com.yeshwanthk.agentic_url_shortener.orchestration.dto;

import com.yeshwanthk.agentic_url_shortener.orchestration.domain.ScenarioType;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.Workflow;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNode;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNodeStatus;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowStage;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID id,
        String name,
        String requirement,
        ScenarioType scenarioType,
        WorkflowStatus status,
        int revision,
        List<NodeResponse> nodes,
        Instant createdAt,
        Instant completedAt
) {

    public static WorkflowResponse from(Workflow workflow) {
        return new WorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getRequirement(),
                workflow.getScenarioType(),
                workflow.getStatus(),
                workflow.getRevision(),
                workflow.getNodes().stream()
                        .map(NodeResponse::from)
                        .toList(),
                workflow.getCreatedAt(),
                workflow.getCompletedAt()
        );
    }

    public record NodeResponse(
            UUID id,
            String nodeKey,
            WorkflowStage stage,
            WorkflowNodeStatus status,
            String output,
            Instant startedAt,
            Instant completedAt
    ) {
        static NodeResponse from(WorkflowNode node) {
            return new NodeResponse(
                    node.getId(),
                    node.getNodeKey(),
                    node.getStage(),
                    node.getStatus(),
                    node.getOutput(),
                    node.getStartedAt(),
                    node.getCompletedAt()
            );
        }
    }
}