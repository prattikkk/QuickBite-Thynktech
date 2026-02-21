-- V16: Enable promo-engine feature flag and seed sample promo codes
-- The promo-engine was seeded as 'false' in V11; enable it by default so promos work out of the box.

UPDATE feature_flags SET enabled = true, updated_at = NOW() WHERE flag_key = 'promo-engine';

-- Seed sample promo codes for testing / demo
INSERT INTO promo_codes (id, code, description, discount_type, discount_value, min_order_cents, max_discount_cents, max_uses, current_uses, valid_from, valid_until, active, created_at)
VALUES
  (gen_random_uuid(), 'WELCOME50', '50% off your first order (max ₹250 off)', 'PERCENT', 5000, 20000, 25000, 1000, 0, NOW(), '2026-12-31 23:59:59+00', true, NOW()),
  (gen_random_uuid(), 'FLAT100',   '₹100 off on orders above ₹300',           'FIXED',   10000, 30000, NULL,  500,  0, NOW(), '2026-12-31 23:59:59+00', true, NOW()),
  (gen_random_uuid(), 'NEWUSER20', '20% off for new users (max ₹150 off)',     'PERCENT', 2000, 15000, 15000, 2000, 0, NOW(), '2026-12-31 23:59:59+00', true, NOW())
ON CONFLICT (code) DO NOTHING;
