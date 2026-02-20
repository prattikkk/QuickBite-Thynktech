-- V5: Add webhook_events table for idempotency and payment tracking improvements

-- Create webhook_events table for tracking processed webhooks (idempotency)
CREATE TABLE IF NOT EXISTS webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100),
    payload TEXT,
    processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ
);

-- Create indexes for webhook_events
CREATE INDEX IF NOT EXISTS idx_webhook_provider_event_id ON webhook_events(provider_event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_created ON webhook_events(created_at);
CREATE INDEX IF NOT EXISTS idx_webhook_processed ON webhook_events(processed, created_at);

-- Add comments
COMMENT ON TABLE webhook_events IS 'Stores processed webhook events for idempotency and audit trail';
COMMENT ON COLUMN webhook_events.provider_event_id IS 'Unique event ID from payment provider';
COMMENT ON COLUMN webhook_events.processed IS 'Whether the webhook has been successfully processed';
