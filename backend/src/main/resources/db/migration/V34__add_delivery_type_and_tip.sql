-- V34: Add delivery_type (PICKUP vs DELIVERY) and tip_cents to orders
-- Also add customer_avatar_url field to order response support

-- 1. delivery_type column with default DELIVERY for existing orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_type VARCHAR(20) NOT NULL DEFAULT 'DELIVERY';

-- 2. tip_cents column (post-delivery tipping)
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tip_cents BIGINT NOT NULL DEFAULT 0;

-- 3. Index for delivery_type filtering
CREATE INDEX IF NOT EXISTS idx_order_delivery_type ON orders(delivery_type);
