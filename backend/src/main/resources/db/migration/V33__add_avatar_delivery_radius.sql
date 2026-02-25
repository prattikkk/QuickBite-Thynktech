-- V33: Add avatar_url to users, delivery_radius_km to vendors
-- Profile picture upload + delivery area config features

ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

ALTER TABLE vendors ADD COLUMN IF NOT EXISTS delivery_radius_km NUMERIC(6,2);
