package com.yeshwanthk.agentic_url_shortener.orchestration.repository;

import com.yeshwanthk.agentic_url_shortener.orchestration.domain.Workflow;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRepository
        extends JpaRepository<Workflow, UUID> {

    @EntityGraph(attributePaths = "nodes")
    Optional<Workflow> findDetailedById(UUID id);
}