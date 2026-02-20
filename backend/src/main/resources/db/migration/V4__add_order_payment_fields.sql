-- V4__add_order_payment_fields.sql
-- Add missing fields to orders table for Day 4 implementation

-- Add new columns to orders table
ALTER TABLE orders ADD COLUMN IF NOT EXISTS order_number VARCHAR(50) UNIQUE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS subtotal_cents BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_fee_cents BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tax_cents BIGINT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_method VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_status VARCHAR(50);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS payment_id UUID;

-- Add foreign key constraint for payment_id
ALTER TABLE orders ADD CONSTRAINT fk_order_payment 
    FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL;

-- Fix delivery_status table to use changed_by_user_id instead of changed_by
ALTER TABLE delivery_status DROP CONSTRAINT IF EXISTS fk_deliverystatus_user;
ALTER TABLE delivery_status RENAME COLUMN changed_by TO changed_by_user_id;
ALTER TABLE delivery_status ADD COLUMN IF NOT EXISTS note TEXT;

-- Update existing orders to have calculated values (for development only)
UPDATE orders 
SET subtotal_cents = COALESCE(subtotal_cents, 0),
    delivery_fee_cents = COALESCE(delivery_fee_cents, 5000),
    tax_cents = COALESCE(tax_cents, 0)
WHERE subtotal_cents IS NULL OR delivery_fee_cents IS NULL OR tax_cents IS NULL;

-- Make new columns NOT NULL after setting defaults
ALTER TABLE orders ALTER COLUMN subtotal_cents SET NOT NULL;
ALTER TABLE orders ALTER COLUMN delivery_fee_cents SET NOT NULL;
ALTER TABLE orders ALTER COLUMN tax_cents SET NOT NULL;

-- Create index on order_number for fast lookups
CREATE INDEX IF NOT EXISTS idx_order_number ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_order_payment ON orders(payment_id);
