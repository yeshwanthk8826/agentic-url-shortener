CREATE TABLE short_urls
(
    id             UUID         NOT NULL,
    short_code     VARCHAR(16)  NOT NULL,
    original_url   TEXT         NOT NULL,
    normalized_url TEXT         NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    expires_at     TIMESTAMPTZ,
    version        BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_short_urls
        PRIMARY KEY (id),

    CONSTRAINT uk_short_urls_short_code
        UNIQUE (short_code),

    CONSTRAINT ck_short_urls_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),

    CONSTRAINT ck_short_urls_expiration
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE INDEX idx_short_urls_normalized_url
    ON short_urls (normalized_url);

CREATE INDEX idx_short_urls_expiration
    ON short_urls (expires_at)
    WHERE expires_at IS NOT NULL;