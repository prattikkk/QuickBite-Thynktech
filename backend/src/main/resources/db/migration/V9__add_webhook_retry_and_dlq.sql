-- V9: Add retry columns to webhook_events and create webhook dead-letter queue
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS attempts     INT         NOT NULL DEFAULT 0;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS max_attempts INT         NOT NULL DEFAULT 5;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS last_error   TEXT;
ALTER TABLE webhook_events ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_webhook_events_pending ON webhook_events (processed, next_retry_at)
    WHERE processed = FALSE;

CREATE TABLE IF NOT EXISTS webhook_dlq (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_event_id VARCHAR(255) NOT NULL,
    event_type        VARCHAR(100),
    payload           TEXT         NOT NULL,
    error_message     TEXT,
    attempts          INT          NOT NULL DEFAULT 0,
    original_event_id UUID         REFERENCES webhook_events(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_dlq_created ON webhook_dlq (created_at DESC);
