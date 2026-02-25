-- V28: Inventory management for menu items (Phase 4.6)

ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS stock_count INT DEFAULT -1;  -- -1 = unlimited
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS low_stock_threshold INT DEFAULT 5;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS auto_disable_on_zero BOOLEAN DEFAULT TRUE;
ALTER TABLE menu_items ADD COLUMN IF NOT EXISTS daily_stock_reset INT DEFAULT -1;  -- -1 = no reset

CREATE INDEX idx_menu_items_stock ON menu_items(vendor_id, stock_count) WHERE stock_count >= 0;
