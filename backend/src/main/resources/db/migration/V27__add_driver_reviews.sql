-- V27: Driver ratings/reviews by customers (Phase 4.7)

CREATE TABLE driver_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) NOT NULL,
    customer_id UUID REFERENCES users(id) NOT NULL,
    driver_id UUID REFERENCES users(id) NOT NULL,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    disputed BOOLEAN DEFAULT FALSE,
    dispute_reason TEXT,
    hidden BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(order_id, customer_id)
);

-- Add driver average rating column for quick queries
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS avg_rating NUMERIC(3,2) DEFAULT 0;
ALTER TABLE driver_profiles ADD COLUMN IF NOT EXISTS total_ratings INT DEFAULT 0;

CREATE INDEX idx_driver_reviews_driver ON driver_reviews(driver_id);
CREATE INDEX idx_driver_reviews_order ON driver_reviews(order_id);
