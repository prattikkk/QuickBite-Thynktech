-- V6: Normalize legacy order status values to current enum names.
-- 'COMPLETED' was renamed to 'DELIVERED' in the OrderStatus enum.
-- 'CONFIRMED' was renamed to 'ACCEPTED' in the OrderStatus enum.
-- This migration ensures all existing rows use the current names.

UPDATE orders
SET status = 'DELIVERED'
WHERE status = 'COMPLETED';

UPDATE delivery_status
SET status = 'ACCEPTED'
WHERE status = 'CONFIRMED';
