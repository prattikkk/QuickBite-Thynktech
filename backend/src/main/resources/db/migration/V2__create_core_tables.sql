-- V2__create_core_tables.sql
-- QuickBite Day 2: Core tables with UUID primary keys

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Drop existing tables from V1 (development environment - data loss acceptable)
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

-- Roles table with UUID
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Users table with UUID
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    name VARCHAR(255),
    phone VARCHAR(20),
    role_id UUID NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- Vendors table
CREATE TABLE IF NOT EXISTS vendors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    address TEXT,
    lat NUMERIC(10, 7),
    lng NUMERIC(10, 7),
    open_hours JSONB,
    rating NUMERIC(3, 2) DEFAULT 0.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_vendor_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Menu Items table
CREATE TABLE IF NOT EXISTS menu_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendor_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    available BOOLEAN DEFAULT TRUE,
    prep_time_mins INTEGER DEFAULT 15,
    category VARCHAR(100),
    image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_menuitem_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE CASCADE
);

-- Addresses table
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    lat NUMERIC(10, 7),
    lng NUMERIC(10, 7),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    vendor_id UUID NOT NULL,
    driver_id UUID,
    delivery_address_id UUID,
    total_cents BIGINT NOT NULL CHECK (total_cents >= 0),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    scheduled_time TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    special_instructions TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_vendor FOREIGN KEY (vendor_id) REFERENCES vendors(id) ON DELETE RESTRICT,
    CONSTRAINT fk_order_driver FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_order_address FOREIGN KEY (delivery_address_id) REFERENCES addresses(id) ON DELETE SET NULL
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    menu_item_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    special_instructions TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_menuitem FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE RESTRICT
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents >= 0),
    currency VARCHAR(10) DEFAULT 'INR',
    provider VARCHAR(50),
    provider_payment_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMPTZ,
    failed_at TIMESTAMPTZ,
    failure_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Delivery Status table (tracking order state changes)
CREATE TABLE IF NOT EXISTS delivery_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMPTZ DEFAULT NOW(),
    location_lat NUMERIC(10, 7),
    location_lng NUMERIC(10, 7),
    note TEXT,
    CONSTRAINT fk_deliverystatus_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_deliverystatus_user FOREIGN KEY (changed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Token Store table (for JWT refresh tokens)
CREATE TABLE IF NOT EXISTS token_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    token_type VARCHAR(50) DEFAULT 'REFRESH',
    issued_at TIMESTAMPTZ DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    device_info TEXT,
    ip_address VARCHAR(45),
    CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Audit Logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity VARCHAR(100),
    entity_id UUID,
    old_values JSONB,
    new_values JSONB,
    meta JSONB,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT fk_auditlog_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_role ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_user_active ON users(active);

CREATE INDEX IF NOT EXISTS idx_vendor_name ON vendors(name);
CREATE INDEX IF NOT EXISTS idx_vendor_user ON vendors(user_id);
CREATE INDEX IF NOT EXISTS idx_vendor_active ON vendors(active);
CREATE INDEX IF NOT EXISTS idx_vendor_location ON vendors(lat, lng);

CREATE INDEX IF NOT EXISTS idx_menuitem_vendor ON menu_items(vendor_id);
CREATE INDEX IF NOT EXISTS idx_menuitem_available ON menu_items(available);
CREATE INDEX IF NOT EXISTS idx_menuitem_category ON menu_items(category);

CREATE INDEX IF NOT EXISTS idx_address_user ON addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_address_default ON addresses(user_id, is_default) WHERE is_default = TRUE;

CREATE INDEX IF NOT EXISTS idx_order_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_vendor ON orders(vendor_id);
CREATE INDEX IF NOT EXISTS idx_order_driver ON orders(driver_id);
CREATE INDEX IF NOT EXISTS idx_order_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_created ON orders(created_at);
CREATE INDEX IF NOT EXISTS idx_order_customer_status ON orders(customer_id, status);

CREATE INDEX IF NOT EXISTS idx_orderitem_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_orderitem_menuitem ON order_items(menu_item_id);

CREATE INDEX IF NOT EXISTS idx_payment_order ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payment_provider_id ON payments(provider_payment_id);

CREATE INDEX IF NOT EXISTS idx_deliverystatus_order ON delivery_status(order_id);
CREATE INDEX IF NOT EXISTS idx_deliverystatus_changed_at ON delivery_status(changed_at);

CREATE INDEX IF NOT EXISTS idx_token_user ON token_store(user_id);
CREATE INDEX IF NOT EXISTS idx_token_hash ON token_store(token_hash);
CREATE INDEX IF NOT EXISTS idx_token_expires ON token_store(expires_at);
CREATE INDEX IF NOT EXISTS idx_token_revoked ON token_store(revoked) WHERE revoked = FALSE;

CREATE INDEX IF NOT EXISTS idx_auditlog_user ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_auditlog_entity ON audit_logs(entity, entity_id);
CREATE INDEX IF NOT EXISTS idx_auditlog_created ON audit_logs(created_at);

-- Comments for documentation
COMMENT ON TABLE roles IS 'User roles: ADMIN, VENDOR, CUSTOMER, DRIVER';
COMMENT ON TABLE users IS 'All system users including customers, vendors, drivers, and admins';
COMMENT ON TABLE vendors IS 'Vendor/Restaurant profiles linked to user accounts';
COMMENT ON TABLE menu_items IS 'Food items offered by vendors';
COMMENT ON TABLE addresses IS 'User delivery addresses';
COMMENT ON TABLE orders IS 'Customer orders with status tracking';
COMMENT ON TABLE order_items IS 'Line items within an order';
COMMENT ON TABLE payments IS 'Payment transactions for orders';
COMMENT ON TABLE delivery_status IS 'Order status change audit trail with location tracking';
COMMENT ON TABLE token_store IS 'JWT refresh tokens for authentication';
COMMENT ON TABLE audit_logs IS 'System-wide audit trail for security and compliance';
