-- Phase 2: Feature flags table for runtime feature toggle
CREATE TABLE IF NOT EXISTS feature_flags (
    flag_key    VARCHAR(100)  PRIMARY KEY,
    enabled     BOOLEAN       NOT NULL DEFAULT false,
    description VARCHAR(500),
    updated_at  TIMESTAMPTZ   DEFAULT now(),
    updated_by  VARCHAR(255)
);

-- Seed default flags matching application.properties
INSERT INTO feature_flags (flag_key, enabled, description) VALUES
    ('driver-auto-assign', true,  'Automatic driver assignment on READY status'),
    ('promo-engine',       false, 'Promotional coupon engine (Phase 3)'),
    ('webhook-async-processing', true, 'Async webhook retry processing'),
    ('structured-logging', true,  'JSON structured logging in prod profile')
ON CONFLICT (flag_key) DO NOTHING;
