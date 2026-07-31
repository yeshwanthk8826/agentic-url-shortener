package com.yeshwanthk.agentic_url_shortener.orchestration.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class WorkflowDependencyRepository {

    private final JdbcClient jdbcClient;

    public WorkflowDependencyRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void add(
            UUID workflowId,
            UUID prerequisiteId,
            UUID dependentId
    ) {
        jdbcClient.sql("""
                INSERT INTO workflow_dependencies (
                    workflow_id,
                    prerequisite_node_id,
                    dependent_node_id
                )
                VALUES (
                    :workflowId,
                    :prerequisiteId,
                    :dependentId
                )
                """)
                .param("workflowId", workflowId)
                .param("prerequisiteId", prerequisiteId)
                .param("dependentId", dependentId)
                .update();
    }

    public List<UUID> findPrerequisiteIds(UUID dependentId) {
        return jdbcClient.sql("""
                SELECT prerequisite_node_id
                  FROM workflow_dependencies
                 WHERE dependent_node_id = :dependentId
                """)
                .param("dependentId", dependentId)
                .query(UUID.class)
                .list();
    }

    public List<UUID> findDependentIds(UUID prerequisiteId) {
        return jdbcClient.sql("""
                SELECT dependent_node_id
                  FROM workflow_dependencies
                 WHERE prerequisite_node_id = :prerequisiteId
                """)
                .param("prerequisiteId", prerequisiteId)
                .query(UUID.class)
                .list();
    }
}