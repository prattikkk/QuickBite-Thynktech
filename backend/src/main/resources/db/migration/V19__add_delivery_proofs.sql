-- V19: Proof-of-delivery table + feature flags + notification type seeds
-- Phase 3 — Proof-of-Delivery & Notifications

-- ── delivery_proofs table ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS delivery_proofs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    driver_id       UUID NOT NULL REFERENCES users(id),
    proof_type      VARCHAR(20) NOT NULL DEFAULT 'PHOTO',  -- PHOTO, OTP, SIGNATURE
    photo_url       TEXT,
    otp_code        VARCHAR(6),
    otp_verified    BOOLEAN DEFAULT FALSE,
    notes           TEXT,
    lat             NUMERIC(10,8),
    lng             NUMERIC(11,8),
    submitted_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_delivery_proofs_order ON delivery_proofs(order_id);
CREATE INDEX idx_delivery_proofs_driver ON delivery_proofs(driver_id);

-- ── Feature flags for Phase 3 ─────────────────────────────────
INSERT INTO feature_flags (flag_key, enabled, description, updated_by)
VALUES
  ('proof-of-delivery', true, 'Enable proof-of-delivery flow (photo/OTP)', 'migration'),
  ('delivery-otp-required', false, 'Require OTP verification before marking delivered', 'migration'),
  ('delivery-photo-required', true, 'Require photo before marking delivered', 'migration'),
  ('delivery-notifications', true, 'Send notifications on all order status transitions', 'migration')
ON CONFLICT (flag_key) DO NOTHING;

-- ── Add delivery_proof_id reference to orders ──────────────────
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_proof_id UUID REFERENCES delivery_proofs(id);
