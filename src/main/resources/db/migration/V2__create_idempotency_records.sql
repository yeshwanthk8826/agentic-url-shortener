CREATE TABLE idempotency_records
(
    idempotency_key VARCHAR(128) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    response_status INTEGER,
    response_body   TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    expires_at      TIMESTAMPTZ  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_idempotency_records
        PRIMARY KEY (idempotency_key),

    CONSTRAINT ck_idempotency_records_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),

    CONSTRAINT ck_idempotency_completed_response
        CHECK (
            status <> 'COMPLETED'
            OR (
                response_status IS NOT NULL
                AND response_body IS NOT NULL
            )
        ),

    CONSTRAINT ck_idempotency_expiration
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_idempotency_records_expires_at
    ON idempotency_records (expires_at);