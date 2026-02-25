-- V26: Modifier groups and modifiers for menu items (Phase 4.12)

CREATE TABLE modifier_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_item_id UUID REFERENCES menu_items(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(100) NOT NULL,            -- e.g. "Size", "Toppings", "Extra"
    required BOOLEAN DEFAULT FALSE,
    min_selections INT DEFAULT 0,
    max_selections INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID REFERENCES modifier_groups(id) ON DELETE CASCADE NOT NULL,
    name VARCHAR(100) NOT NULL,            -- e.g. "Large", "Extra Cheese"
    price_cents BIGINT DEFAULT 0,          -- additional price
    available BOOLEAN DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Track selected modifiers per order item
CREATE TABLE order_item_modifiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID REFERENCES order_items(id) ON DELETE CASCADE NOT NULL,
    modifier_id UUID REFERENCES modifiers(id) NOT NULL,
    modifier_name VARCHAR(100) NOT NULL,   -- snapshot at time of order
    price_cents BIGINT DEFAULT 0           -- snapshot at time of order
);

CREATE INDEX idx_modifier_groups_item ON modifier_groups(menu_item_id);
CREATE INDEX idx_modifiers_group ON modifiers(group_id);
CREATE INDEX idx_order_item_modifiers_item ON order_item_modifiers(order_item_id);
