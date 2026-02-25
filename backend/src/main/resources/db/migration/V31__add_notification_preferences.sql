-- V31: Add notification_preferences table
-- Stores per-user notification channel toggles

CREATE TABLE IF NOT EXISTS notification_preferences (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    push_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    email_order_updates   BOOLEAN NOT NULL DEFAULT TRUE,
    email_promotions      BOOLEAN NOT NULL DEFAULT FALSE,
    sms_delivery_alerts   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_prefs_user
    ON notification_preferences(user_id);
