-- V17: Add driver_profiles table for Phase 1 driver dashboard enhancement
-- Stores driver vehicle info, online/offline status, last-known GPS, and delivery stats

CREATE TABLE driver_profiles (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    vehicle_type    VARCHAR(50),         -- BICYCLE, MOTORCYCLE, CAR, VAN
    license_plate   VARCHAR(50),
    is_online       BOOLEAN NOT NULL DEFAULT FALSE,
    current_lat     DECIMAL(10,8),
    current_lng     DECIMAL(11,8),
    last_seen_at    TIMESTAMPTZ,
    total_deliveries INT NOT NULL DEFAULT 0,
    success_rate    DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_driver_profiles_user ON driver_profiles(user_id);
CREATE INDEX idx_driver_profiles_online ON driver_profiles(is_online);

-- Seed driver profile for the existing seed driver user
INSERT INTO driver_profiles (user_id, vehicle_type, is_online, total_deliveries, success_rate)
SELECT u.id, 'MOTORCYCLE', false, 0, 100.00
FROM users u JOIN roles r ON u.role_id = r.id
WHERE r.name = 'DRIVER'
ON CONFLICT (user_id) DO NOTHING;
