package com.yeshwanthk.agentic_url_shortener.orchestration.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workflow_nodes")
public class WorkflowNode {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "node_key", nullable = false, length = 50)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkflowStage stage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowNodeStatus status;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected WorkflowNode() {
        // Required by JPA.
    }

    WorkflowNode(
            Workflow workflow,
            String nodeKey,
            WorkflowStage stage,
            WorkflowNodeStatus status,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.workflow = Objects.requireNonNull(workflow);
        this.nodeKey = Objects.requireNonNull(nodeKey);
        this.stage = Objects.requireNonNull(stage);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
    }

    public void markReady(Instant instant) {
        if (status != WorkflowNodeStatus.BLOCKED
                && status != WorkflowNodeStatus.INVALIDATED) {
            return;
        }

        status = WorkflowNodeStatus.READY;
        updatedAt = instant;
    }

    public void start(Instant instant) {
        if (status != WorkflowNodeStatus.READY) {
            throw new IllegalStateException(
                    "Only a ready node can be started"
            );
        }

        status = WorkflowNodeStatus.RUNNING;
        startedAt = instant;
        updatedAt = instant;
    }

    public void complete(String output, Instant instant) {
        if (status != WorkflowNodeStatus.READY
                && status != WorkflowNodeStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a ready or running node can be completed"
            );
        }

        if (output == null || output.isBlank()) {
            throw new IllegalArgumentException(
                    "Completed node output must not be blank"
            );
        }

        this.status = WorkflowNodeStatus.COMPLETED;
        this.output = output;
        this.completedAt = instant;
        this.updatedAt = instant;

        if (startedAt == null) {
            startedAt = instant;
        }
    }

    public void invalidate(Instant instant) {
        status = WorkflowNodeStatus.INVALIDATED;
        output = null;
        startedAt = null;
        completedAt = null;
        updatedAt = instant;
    }

    public void block(Instant instant) {
        status = WorkflowNodeStatus.BLOCKED;
        output = null;
        startedAt = null;
        completedAt = null;
        updatedAt = instant;
    }

    public boolean isCompleted() {
        return status == WorkflowNodeStatus.COMPLETED;
    }

    public boolean isReady() {
        return status == WorkflowNodeStatus.READY;
    }

    public UUID getId() {
        return id;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public WorkflowStage getStage() {
        return stage;
    }

    public WorkflowNodeStatus getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
