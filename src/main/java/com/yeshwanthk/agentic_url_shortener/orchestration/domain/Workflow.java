package com.yeshwanthk.agentic_url_shortener.orchestration.domain;

import com.yeshwanthk.agentic_url_shortener.orchestration.exception.WorkflowStateException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "workflows")
public class Workflow {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 20)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status;

    @Column(nullable = false)
    private int revision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    @OneToMany(
            mappedBy = "workflow",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @OrderBy("createdAt ASC")
    private List<WorkflowNode> nodes = new ArrayList<>();

    protected Workflow() {
        // Required by JPA.
    }

    public Workflow(
            String name,
            String requirement,
            ScenarioType scenarioType,
            Instant createdAt
    ) {
        this.id = UUID.randomUUID();
        this.name = requireText(name, "Workflow name");
        this.requirement = requireText(
                requirement,
                "Workflow requirement"
        );
        this.scenarioType = Objects.requireNonNull(scenarioType);
        this.status = WorkflowStatus.PLANNED;
        this.revision = 1;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = createdAt;
    }

    public WorkflowNode addNode(
            String nodeKey,
            WorkflowStage stage,
            WorkflowNodeStatus status,
            Instant instant
    ) {
        boolean duplicate = nodes.stream()
                .anyMatch(node -> node.getNodeKey().equals(nodeKey));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Duplicate workflow node key: " + nodeKey
            );
        }

        WorkflowNode node = new WorkflowNode(
                this,
                nodeKey,
                stage,
                status,
                instant
        );

        nodes.add(node);
        return node;
    }

    public WorkflowNode getNode(String nodeKey) {
        return nodes.stream()
                .filter(node -> node.getNodeKey().equals(nodeKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Workflow node not found: " + nodeKey
                ));
    }

    public void markRunning(Instant instant) {
        ensureExecutable();

        if (status == WorkflowStatus.PLANNED) {
            status = WorkflowStatus.RUNNING;
            updatedAt = instant;
        }
    }

    public void markCompleted(Instant instant) {
        if (nodes.stream().anyMatch(node -> !node.isCompleted())) {
            throw new IllegalStateException(
                    "Workflow cannot complete while nodes remain incomplete"
            );
        }

        status = WorkflowStatus.COMPLETED;
        completedAt = instant;
        updatedAt = instant;
    }

    public void incrementRevision(Instant instant) {
        revision++;
        status = WorkflowStatus.RUNNING;
        completedAt = null;
        updatedAt = instant;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRequirement() {
        return requirement;
    }

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public int getRevision() {
        return revision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<WorkflowNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    private static String requireText(
            String value,
            String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    public void ensureExecutable() {
        if (status == WorkflowStatus.SAFE_STOPPED) {
            throw new WorkflowStateException(
                    "Workflow is safe-stopped and cannot execute"
            );
        }

        if (status == WorkflowStatus.COMPLETED) {
            throw new WorkflowStateException(
                    "Completed workflow cannot execute"
            );
        }

        if (status == WorkflowStatus.FAILED) {
            throw new WorkflowStateException(
                    "Failed workflow cannot execute"
            );
        }
    }

    public void safeStop(Instant instant) {
        if (status == WorkflowStatus.COMPLETED) {
            throw new WorkflowStateException(
                    "Completed workflow cannot be safe-stopped"
            );
        }

        status = WorkflowStatus.SAFE_STOPPED;
        updatedAt = instant;
    }
}
