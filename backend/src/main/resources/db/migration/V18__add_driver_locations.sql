-- =========================================================================
-- V18 — Driver location history table (Phase 2: Foreground Live Location)
-- Stores GPS sample points so we can track recent driver movement.
-- A separate background job or TTL policy should prune rows older than 24h.
-- =========================================================================

CREATE TABLE driver_locations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    driver_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lat             DECIMAL(10,8) NOT NULL,
    lng             DECIMAL(11,8) NOT NULL,
    accuracy        DOUBLE PRECISION,          -- meters (from Geolocation API)
    speed           DOUBLE PRECISION,          -- m/s   (from Geolocation API)
    heading         DOUBLE PRECISION,          -- degrees (from Geolocation API)
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for fast lookups of a driver's recent locations
CREATE INDEX idx_driver_locations_driver_time ON driver_locations(driver_id, recorded_at DESC);

-- Index for pruning old entries
CREATE INDEX idx_driver_locations_recorded ON driver_locations(recorded_at);

-- Add shift tracking columns to driver_profiles
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS shift_started_at TIMESTAMPTZ;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS shift_ended_at   TIMESTAMPTZ;
