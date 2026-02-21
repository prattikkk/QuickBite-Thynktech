-- V12: Add favorites table for customer vendor bookmarks
CREATE TABLE favorites (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vendor_id   UUID NOT NULL REFERENCES vendors(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, vendor_id)
);

CREATE INDEX idx_favorite_user   ON favorites(user_id);
CREATE INDEX idx_favorite_vendor ON favorites(vendor_id);
