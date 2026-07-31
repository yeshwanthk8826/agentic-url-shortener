ALTER TABLE short_urls
    ADD COLUMN visit_count BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN last_accessed_at TIMESTAMPTZ;

CREATE INDEX idx_short_urls_status_expiration
    ON short_urls (status, expires_at);