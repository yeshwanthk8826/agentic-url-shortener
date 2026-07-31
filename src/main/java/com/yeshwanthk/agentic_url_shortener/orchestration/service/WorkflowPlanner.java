package com.yeshwanthk.agentic_url_shortener.orchestration.service;

import com.yeshwanthk.agentic_url_shortener.orchestration.domain.Workflow;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNode;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNodeStatus;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowStage;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.CreateWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.repository.WorkflowDependencyRepository;
import com.yeshwanthk.agentic_url_shortener.orchestration.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class WorkflowPlanner {

    private final WorkflowRepository workflowRepository;
    private final WorkflowDependencyRepository dependencyRepository;
    private final Clock clock;

    public WorkflowPlanner(
            WorkflowRepository workflowRepository,
            WorkflowDependencyRepository dependencyRepository,
            Clock clock
    ) {
        this.workflowRepository = workflowRepository;
        this.dependencyRepository = dependencyRepository;
        this.clock = clock;
    }

    @Transactional
    public Workflow create(CreateWorkflowRequest request) {
        Instant now = clock.instant();

        Workflow workflow = new Workflow(
                request.name(),
                request.requirement(),
                request.scenarioType(),
                now
        );

        WorkflowNode requirements = workflow.addNode(
                "requirements",
                WorkflowStage.REQUIREMENTS,
                WorkflowNodeStatus.READY,
                now
        );

        WorkflowNode architecture = workflow.addNode(
                "architecture",
                WorkflowStage.ARCHITECTURE,
                WorkflowNodeStatus.BLOCKED,
                now
        );

        WorkflowNode implementation = workflow.addNode(
                "implementation",
                WorkflowStage.IMPLEMENTATION,
                WorkflowNodeStatus.BLOCKED,
                now
        );

        WorkflowNode testing = workflow.addNode(
                "testing",
                WorkflowStage.TESTING,
                WorkflowNodeStatus.BLOCKED,
                now
        );

        WorkflowNode documentation = workflow.addNode(
                "documentation",
                WorkflowStage.DOCUMENTATION,
                WorkflowNodeStatus.BLOCKED,
                now
        );

        WorkflowNode release = workflow.addNode(
                "release-readiness",
                WorkflowStage.RELEASE_READINESS,
                WorkflowNodeStatus.BLOCKED,
                now
        );

        workflowRepository.saveAndFlush(workflow);

        addDependency(workflow, requirements, architecture);
        addDependency(workflow, architecture, implementation);
        addDependency(workflow, implementation, testing);
        addDependency(workflow, implementation, documentation);
        addDependency(workflow, testing, release);
        addDependency(workflow, documentation, release);

        return workflow;
    }

    private void addDependency(
            Workflow workflow,
            WorkflowNode prerequisite,
            WorkflowNode dependent
    ) {
        dependencyRepository.add(
                workflow.getId(),
                prerequisite.getId(),
                dependent.getId()
        );
    }
}