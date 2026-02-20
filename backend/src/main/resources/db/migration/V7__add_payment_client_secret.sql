-- V7: Add client_secret column to payments table for Stripe PaymentIntent
-- The client_secret is used by the frontend to confirm card payments via Stripe.js

ALTER TABLE payments ADD COLUMN IF NOT EXISTS client_secret TEXT;
