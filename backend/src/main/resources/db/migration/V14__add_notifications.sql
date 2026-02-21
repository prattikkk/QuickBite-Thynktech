-- V14: Notification system
CREATE TABLE notifications (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50)  NOT NULL,   -- ORDER_UPDATE, PROMO, SYSTEM, etc.
    title       VARCHAR(255) NOT NULL,
    message     TEXT         NOT NULL,
    ref_id      UUID,                    -- optional reference (order id, promo id, etc.)
    is_read     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_user      ON notifications(user_id);
CREATE INDEX idx_notification_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notification_created   ON notifications(created_at);
