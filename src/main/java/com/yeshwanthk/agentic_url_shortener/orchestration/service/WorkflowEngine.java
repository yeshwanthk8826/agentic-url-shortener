package com.yeshwanthk.agentic_url_shortener.orchestration.service;

import com.yeshwanthk.agentic_url_shortener.orchestration.domain.Workflow;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNode;
import com.yeshwanthk.agentic_url_shortener.orchestration.domain.WorkflowNodeStatus;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.CompleteNodeRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.CreateWorkflowRequest;
import com.yeshwanthk.agentic_url_shortener.orchestration.dto.WorkflowResponse;
import com.yeshwanthk.agentic_url_shortener.orchestration.exception.WorkflowNotFoundException;
import com.yeshwanthk.agentic_url_shortener.orchestration.repository.WorkflowDependencyRepository;
import com.yeshwanthk.agentic_url_shortener.orchestration.repository.WorkflowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowEngine {

    private final WorkflowRepository workflowRepository;
    private final WorkflowDependencyRepository dependencyRepository;
    private final WorkflowPlanner planner;
    private final Clock clock;

    public WorkflowEngine(
            WorkflowRepository workflowRepository,
            WorkflowDependencyRepository dependencyRepository,
            WorkflowPlanner planner,
            Clock clock
    ) {
        this.workflowRepository = workflowRepository;
        this.dependencyRepository = dependencyRepository;
        this.planner = planner;
        this.clock = clock;
    }

    @Transactional
    public WorkflowResponse create(CreateWorkflowRequest request) {
        return WorkflowResponse.from(planner.create(request));
    }

    @Transactional(readOnly = true)
    public WorkflowResponse get(UUID workflowId) {
        return WorkflowResponse.from(load(workflowId));
    }

    @Transactional
    public WorkflowResponse advance(UUID workflowId) {
        Workflow workflow = load(workflowId);
        unlockReadyNodes(workflow, clock.instant());
        return WorkflowResponse.from(workflowRepository.save(workflow));
    }

    @Transactional
    public WorkflowResponse completeNode(
            UUID workflowId,
            String nodeKey,
            CompleteNodeRequest request
    ) {
        Workflow workflow = load(workflowId);
        Instant now = clock.instant();

        workflow.getNode(nodeKey).complete(request.output(), now);
        unlockReadyNodes(workflow, now);

        return WorkflowResponse.from(workflowRepository.save(workflow));
    }

    @Transactional
    public WorkflowResponse replan(
            UUID workflowId,
            String changedNodeKey
    ) {
        Workflow workflow = load(workflowId);
        Instant now = clock.instant();

        WorkflowNode changedNode = workflow.getNode(changedNodeKey);
        changedNode.invalidate(now);
        changedNode.markReady(now);

        Map<UUID, WorkflowNode> nodesById = workflow.getNodes()
                .stream()
                .collect(Collectors.toMap(
                        WorkflowNode::getId,
                        Function.identity()
                ));

        var queue = new ArrayDeque<UUID>();
        queue.add(changedNode.getId());

        while (!queue.isEmpty()) {
            UUID current = queue.remove();

            for (UUID dependentId :
                    dependencyRepository.findDependentIds(current)) {

                WorkflowNode dependent = nodesById.get(dependentId);

                if (dependent != null) {
                    dependent.block(now);
                    queue.add(dependentId);
                }
            }
        }

        workflow.incrementRevision(now);

        return WorkflowResponse.from(workflowRepository.save(workflow));
    }

    private void unlockReadyNodes(
            Workflow workflow,
            Instant now
    ) {
        workflow.markRunning(now);

        Map<UUID, WorkflowNode> nodesById = workflow.getNodes()
                .stream()
                .collect(Collectors.toMap(
                        WorkflowNode::getId,
                        Function.identity()
                ));

        for (WorkflowNode node : workflow.getNodes()) {
            if (node.getStatus() != WorkflowNodeStatus.BLOCKED) {
                continue;
            }

            boolean prerequisitesComplete =
                    dependencyRepository
                            .findPrerequisiteIds(node.getId())
                            .stream()
                            .map(nodesById::get)
                            .allMatch(prerequisite ->
                                    prerequisite != null
                                            && prerequisite.isCompleted()
                            );

            if (prerequisitesComplete) {
                node.markReady(now);
            }
        }

        if (workflow.getNodes().stream()
                .allMatch(WorkflowNode::isCompleted)) {
            workflow.markCompleted(now);
        }
    }

    private Workflow load(UUID workflowId) {
        return workflowRepository.findDetailedById(workflowId)
                .orElseThrow(() ->
                        new WorkflowNotFoundException(workflowId)
                );
    }
}