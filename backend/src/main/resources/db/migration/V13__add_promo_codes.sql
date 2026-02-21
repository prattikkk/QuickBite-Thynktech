-- V13: Promo/coupon engine tables and order discount columns
CREATE TABLE promo_codes (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code            VARCHAR(50)  NOT NULL UNIQUE,
    description     TEXT,
    discount_type   VARCHAR(20)  NOT NULL CHECK (discount_type IN ('PERCENT','FIXED')),
    discount_value  BIGINT       NOT NULL,          -- cents for FIXED, basis-points (x100) for PERCENT (e.g. 1500 = 15%)
    min_order_cents BIGINT       NOT NULL DEFAULT 0, -- minimum subtotal to apply
    max_discount_cents BIGINT,                       -- cap for PERCENT discounts
    max_uses        INT,                             -- NULL = unlimited
    current_uses    INT          NOT NULL DEFAULT 0,
    valid_from      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    valid_until     TIMESTAMPTZ,
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_promo_code   ON promo_codes(code);
CREATE INDEX idx_promo_active ON promo_codes(active);

-- Add discount columns to orders table
ALTER TABLE orders ADD COLUMN promo_code        VARCHAR(50);
ALTER TABLE orders ADD COLUMN discount_cents     BIGINT NOT NULL DEFAULT 0;
