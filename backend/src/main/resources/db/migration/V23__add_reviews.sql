-- V23: Reviews table for customer ratings of vendors
CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    customer_id UUID NOT NULL REFERENCES users(id),
    vendor_id UUID NOT NULL REFERENCES vendors(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    hidden BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(order_id, customer_id)
);

CREATE INDEX idx_review_vendor ON reviews(vendor_id);
CREATE INDEX idx_review_customer ON reviews(customer_id);
CREATE INDEX idx_review_order ON reviews(order_id);
CREATE INDEX idx_review_vendor_rating ON reviews(vendor_id, rating);
