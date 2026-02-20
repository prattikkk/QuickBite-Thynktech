-- V8: Idempotency keys table for safe retries on critical write endpoints
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255)  NOT NULL,
    user_id         UUID          NOT NULL REFERENCES users(id),
    endpoint        VARCHAR(255)  NOT NULL,
    request_hash    VARCHAR(64),
    response_status INT,
    response_body   TEXT,
    used            BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ   NOT NULL DEFAULT (now() + INTERVAL '24 hours'),
    CONSTRAINT uq_idempotency_key_user UNIQUE (idempotency_key, user_id, endpoint)
);

CREATE INDEX idx_idempotency_key_lookup ON idempotency_keys (idempotency_key, user_id, endpoint);
CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);
