-- V10: Event timeline for order audit trail (actor, event_type, metadata)
CREATE TABLE IF NOT EXISTS event_timeline (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL REFERENCES orders(id),
    actor_id    UUID         REFERENCES users(id),
    actor_role  VARCHAR(50),
    event_type  VARCHAR(100) NOT NULL,
    old_status  VARCHAR(50),
    new_status  VARCHAR(50),
    meta        JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_event_timeline_order ON event_timeline (order_id, created_at);
CREATE INDEX idx_event_timeline_actor ON event_timeline (actor_id);
CREATE INDEX idx_event_timeline_type  ON event_timeline (event_type);
