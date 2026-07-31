package com.yeshwanthk.agentic_url_shortener.orchestration.repository;

import com.yeshwanthk.agentic_url_shortener.orchestration.dto.AuditEventResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class WorkflowGovernanceRepository {

    public static final String RELEASE_NODE =
            "release-readiness";

    private final JdbcClient jdbcClient;

    public WorkflowGovernanceRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void approveRelease(
            UUID workflowId,
            String actor,
            String reason,
            Instant approvedAt
    ) {
        jdbcClient.sql("""
                INSERT INTO workflow_approvals (
                    id,
                    workflow_id,
                    node_key,
                    approved_by,
                    reason,
                    approved_at
                )
                VALUES (
                    :id,
                    :workflowId,
                    :nodeKey,
                    :actor,
                    :reason,
                    :approvedAt
                )
                ON CONFLICT (workflow_id, node_key)
                DO UPDATE SET
                    approved_by = EXCLUDED.approved_by,
                    reason = EXCLUDED.reason,
                    approved_at = EXCLUDED.approved_at
                """)
                .param("id", UUID.randomUUID())
                .param("workflowId", workflowId)
                .param("nodeKey", RELEASE_NODE)
                .param("actor", actor)
                .param("reason", reason)
                .param(
                        "approvedAt",
                        toDatabaseTimestamp(approvedAt)
                )
                .update();
    }

    public boolean isReleaseApproved(UUID workflowId) {
        return jdbcClient.sql("""
                SELECT EXISTS (
                    SELECT 1
                      FROM workflow_approvals
                     WHERE workflow_id = :workflowId
                       AND node_key = :nodeKey
                )
                """)
                .param("workflowId", workflowId)
                .param("nodeKey", RELEASE_NODE)
                .query(Boolean.class)
                .single();
    }

    public void audit(
            UUID workflowId,
            String eventType,
            String actor,
            String details,
            Instant createdAt
    ) {
        jdbcClient.sql("""
                INSERT INTO workflow_audit_events (
                    id,
                    workflow_id,
                    event_type,
                    actor,
                    details,
                    created_at
                )
                VALUES (
                    :id,
                    :workflowId,
                    :eventType,
                    :actor,
                    :details,
                    :createdAt
                )
                """)
                .param("id", UUID.randomUUID())
                .param("workflowId", workflowId)
                .param("eventType", eventType)
                .param("actor", actor)
                .param("details", details)
                .param(
                        "createdAt",
                        toDatabaseTimestamp(createdAt)
                )
                .update();
    }

    public List<AuditEventResponse> findAuditEvents(
            UUID workflowId
    ) {
        return jdbcClient.sql("""
                SELECT
                    id,
                    workflow_id,
                    event_type,
                    actor,
                    details,
                    created_at
                  FROM workflow_audit_events
                 WHERE workflow_id = :workflowId
                 ORDER BY created_at ASC, id ASC
                """)
                .param("workflowId", workflowId)
                .query((resultSet, rowNumber) ->
                        new AuditEventResponse(
                                resultSet.getObject(
                                        "id",
                                        UUID.class
                                ),
                                resultSet.getObject(
                                        "workflow_id",
                                        UUID.class
                                ),
                                resultSet.getString("event_type"),
                                resultSet.getString("actor"),
                                resultSet.getString("details"),
                                resultSet.getObject(
                                        "created_at",
                                        java.time.OffsetDateTime.class
                                ).toInstant()
                        )
                )
                .list();
    }

    private OffsetDateTime toDatabaseTimestamp(Instant instant) {
        return OffsetDateTime.ofInstant(
                instant,
                ZoneOffset.UTC
        );
    }
}