-- V32: Per-user promo usage tracking, vendor commission models, order fraud velocity
-- LOW PRIORITY items from gap analysis

-- ============================================================
-- 1. Per-user promo usage tracking
-- ============================================================
CREATE TABLE IF NOT EXISTS promo_usage (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    promo_id    UUID NOT NULL REFERENCES promo_codes(id) ON DELETE CASCADE,
    order_id    UUID REFERENCES orders(id) ON DELETE SET NULL,
    used_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, promo_id, order_id)
);

CREATE INDEX IF NOT EXISTS idx_promo_usage_user ON promo_usage(user_id);
CREATE INDEX IF NOT EXISTS idx_promo_usage_promo ON promo_usage(promo_id);
CREATE INDEX IF NOT EXISTS idx_promo_usage_user_promo ON promo_usage(user_id, promo_id);

-- Add per-user limit column to promo_codes
ALTER TABLE promo_codes ADD COLUMN IF NOT EXISTS max_uses_per_user INTEGER DEFAULT NULL;

-- Add BOGO discount type support (add new column for BOGO item)
ALTER TABLE promo_codes ADD COLUMN IF NOT EXISTS bogo_item_id UUID REFERENCES menu_items(id) ON DELETE SET NULL;

-- Add first_order_only flag for first-order-only promos
ALTER TABLE promo_codes ADD COLUMN IF NOT EXISTS first_order_only BOOLEAN NOT NULL DEFAULT FALSE;

-- ============================================================
-- 2. Vendor commission configuration
-- ============================================================
CREATE TABLE IF NOT EXISTS vendor_commissions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    vendor_id           UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    commission_rate_bps INTEGER NOT NULL DEFAULT 1500,       -- basis points (1500 = 15%)
    flat_fee_cents      BIGINT NOT NULL DEFAULT 0,           -- flat fee per order in cents
    effective_from      TIMESTAMPTZ NOT NULL DEFAULT now(),
    effective_until     TIMESTAMPTZ,                         -- NULL = currently active
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_vendor_commission_vendor ON vendor_commissions(vendor_id);
CREATE INDEX IF NOT EXISTS idx_vendor_commission_active ON vendor_commissions(vendor_id, effective_from)
    WHERE effective_until IS NULL;

-- Add commission tracking to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS commission_cents BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS vendor_payout_cents BIGINT DEFAULT 0;

-- ============================================================
-- 3. Order fraud velocity config (configurable limits stored in DB)
-- ============================================================
CREATE TABLE IF NOT EXISTS fraud_rules (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_name           VARCHAR(100) NOT NULL UNIQUE,
    rule_type           VARCHAR(50) NOT NULL,    -- VELOCITY, AMOUNT, PATTERN
    threshold_value     BIGINT NOT NULL,
    time_window_minutes INTEGER NOT NULL DEFAULT 60,
    action_type         VARCHAR(50) NOT NULL DEFAULT 'BLOCK',  -- BLOCK, FLAG, LOG
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Default fraud rules
INSERT INTO fraud_rules (rule_name, rule_type, threshold_value, time_window_minutes, action_type) VALUES
    ('max_orders_per_hour', 'VELOCITY', 5, 60, 'BLOCK'),
    ('max_spend_per_day', 'AMOUNT', 5000000, 1440, 'FLAG'),        -- ₹50,000 per day
    ('max_failed_payments_per_hour', 'VELOCITY', 3, 60, 'BLOCK'),
    ('max_cancelled_orders_per_day', 'VELOCITY', 5, 1440, 'FLAG')
ON CONFLICT (rule_name) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_fraud_rules_type ON fraud_rules(rule_type) WHERE enabled = TRUE;

-- ============================================================
-- 4. Data retention tracking
-- ============================================================
CREATE TABLE IF NOT EXISTS data_retention_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    table_name      VARCHAR(100) NOT NULL,
    records_deleted BIGINT NOT NULL DEFAULT 0,
    retention_days  INTEGER NOT NULL,
    executed_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_data_retention_executed ON data_retention_log(executed_at);
