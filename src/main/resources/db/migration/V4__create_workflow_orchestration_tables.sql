CREATE TABLE workflows
(
    id               UUID         NOT NULL,
    name             VARCHAR(150) NOT NULL,
    requirement      TEXT         NOT NULL,
    scenario_type    VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL,
    revision         INTEGER      NOT NULL DEFAULT 1,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    completed_at     TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_workflows
        PRIMARY KEY (id),

    CONSTRAINT ck_workflows_scenario_type
        CHECK (
            scenario_type IN (
                'GREENFIELD',
                'BROWNFIELD',
                'AMBIGUOUS'
            )
        ),

    CONSTRAINT ck_workflows_status
        CHECK (
            status IN (
                'PLANNED',
                'RUNNING',
                'COMPLETED',
                'FAILED',
                'SAFE_STOPPED'
            )
        )
);

CREATE TABLE workflow_nodes
(
    id             UUID         NOT NULL,
    workflow_id    UUID         NOT NULL,
    node_key       VARCHAR(50)  NOT NULL,
    stage          VARCHAR(30)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    output         TEXT,
    started_at     TIMESTAMPTZ,
    completed_at   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    version        BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_workflow_nodes
        PRIMARY KEY (id),

    CONSTRAINT fk_workflow_nodes_workflow
        FOREIGN KEY (workflow_id)
            REFERENCES workflows (id)
            ON DELETE CASCADE,

    CONSTRAINT uk_workflow_nodes_key
        UNIQUE (workflow_id, node_key),

    CONSTRAINT ck_workflow_nodes_stage
        CHECK (
            stage IN (
                'REQUIREMENTS',
                'ARCHITECTURE',
                'IMPLEMENTATION',
                'TESTING',
                'DOCUMENTATION',
                'RELEASE_READINESS'
            )
        ),

    CONSTRAINT ck_workflow_nodes_status
        CHECK (
            status IN (
                'BLOCKED',
                'READY',
                'RUNNING',
                'COMPLETED',
                'INVALIDATED'
            )
        )
);

CREATE TABLE workflow_dependencies
(
    workflow_id          UUID NOT NULL,
    prerequisite_node_id UUID NOT NULL,
    dependent_node_id    UUID NOT NULL,

    CONSTRAINT pk_workflow_dependencies
        PRIMARY KEY (
            workflow_id,
            prerequisite_node_id,
            dependent_node_id
        ),

    CONSTRAINT fk_dependencies_workflow
        FOREIGN KEY (workflow_id)
            REFERENCES workflows (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_dependencies_prerequisite
        FOREIGN KEY (prerequisite_node_id)
            REFERENCES workflow_nodes (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_dependencies_dependent
        FOREIGN KEY (dependent_node_id)
            REFERENCES workflow_nodes (id)
            ON DELETE CASCADE,

    CONSTRAINT ck_dependency_not_self
        CHECK (prerequisite_node_id <> dependent_node_id)
);

CREATE INDEX idx_workflow_nodes_workflow_status
    ON workflow_nodes (workflow_id, status);

CREATE INDEX idx_workflow_dependencies_dependent
    ON workflow_dependencies (dependent_node_id);

CREATE INDEX idx_workflow_dependencies_prerequisite
    ON workflow_dependencies (prerequisite_node_id);