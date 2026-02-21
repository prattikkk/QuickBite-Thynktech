-- V15: ETA estimation fields on orders
ALTER TABLE orders ADD COLUMN estimated_delivery_at TIMESTAMPTZ;
ALTER TABLE orders ADD COLUMN estimated_prep_mins   INT;
