-- V25: Chat rooms and messages for in-app messaging (Phase 4.4)

CREATE TABLE chat_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) NOT NULL,
    participant1_id UUID REFERENCES users(id) NOT NULL,
    participant2_id UUID REFERENCES users(id) NOT NULL,
    room_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER_DRIVER',  -- CUSTOMER_DRIVER, CUSTOMER_VENDOR
    closed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(order_id, participant1_id, participant2_id)
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID REFERENCES chat_rooms(id) NOT NULL,
    sender_id UUID REFERENCES users(id) NOT NULL,
    content TEXT NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_chat_rooms_order ON chat_rooms(order_id);
CREATE INDEX idx_chat_messages_room ON chat_messages(room_id);
CREATE INDEX idx_chat_messages_created ON chat_messages(room_id, created_at DESC);
