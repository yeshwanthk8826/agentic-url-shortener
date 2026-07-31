CREATE TABLE workflow_approvals
(
    id          UUID         NOT NULL,
    workflow_id UUID         NOT NULL,
    node_key    VARCHAR(50)  NOT NULL,
    approved_by VARCHAR(150) NOT NULL,
    reason      TEXT         NOT NULL,
    approved_at TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_workflow_approvals
        PRIMARY KEY (id),

    CONSTRAINT fk_workflow_approvals_workflow
        FOREIGN KEY (workflow_id)
            REFERENCES workflows (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_workflow_approval_node
        UNIQUE (workflow_id, node_key)
);

CREATE TABLE workflow_audit_events
(
    id          UUID         NOT NULL,
    workflow_id UUID         NOT NULL,
    event_type  VARCHAR(50)  NOT NULL,
    actor       VARCHAR(150) NOT NULL,
    details     TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_workflow_audit_events
        PRIMARY KEY (id),

    CONSTRAINT fk_workflow_audit_workflow
        FOREIGN KEY (workflow_id)
            REFERENCES workflows (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_workflow_audit_workflow_created
    ON workflow_audit_events (workflow_id, created_at);