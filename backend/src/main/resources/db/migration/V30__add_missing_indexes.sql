-- V30: Add missing database indexes for query performance
-- Identified by cross-referencing repository queries with existing indexes

-- =====================================================
-- HIGH PRIORITY: Composite indexes for frequent queries
-- =====================================================

-- OrderRepository.findByVendorIdAndStatus / countByVendorIdAndStatus
CREATE INDEX IF NOT EXISTS idx_order_vendor_status
    ON orders(vendor_id, status);

-- OrderRepository.findByDriverIdAndStatus
CREATE INDEX IF NOT EXISTS idx_order_driver_status
    ON orders(driver_id, status);

-- DeliveryStatusRepository.findByOrderIdAndStatus
CREATE INDEX IF NOT EXISTS idx_deliverystatus_order_status
    ON delivery_status(order_id, status);

-- =====================================================
-- HIGH PRIORITY: FK columns without indexes
-- =====================================================

-- FK orders.delivery_address_id -> addresses(id), needed for CASCADE performance
CREATE INDEX IF NOT EXISTS idx_order_delivery_address
    ON orders(delivery_address_id);

-- DeliveryStatusRepository.findByChangedByUserId
CREATE INDEX IF NOT EXISTS idx_deliverystatus_changed_by
    ON delivery_status(changed_by_user_id);

-- =====================================================
-- HIGH PRIORITY: Frequently queried non-FK columns
-- =====================================================

-- AuditLogRepository.findByAction, findByUserIdAndAction
CREATE INDEX IF NOT EXISTS idx_auditlog_action
    ON audit_logs(action);

-- AuditLogRepository.findByIpAddress (security lookups)
CREATE INDEX IF NOT EXISTS idx_auditlog_ip_address
    ON audit_logs(ip_address);

-- =====================================================
-- MEDIUM PRIORITY: Chat FK columns
-- =====================================================

-- ChatRoomRepository.findByParticipant1IdOrParticipant2Id
CREATE INDEX IF NOT EXISTS idx_chat_rooms_participant1
    ON chat_rooms(participant1_id);

CREATE INDEX IF NOT EXISTS idx_chat_rooms_participant2
    ON chat_rooms(participant2_id);

-- ChatMessageRepository mark-as-read query filters by sender_id
CREATE INDEX IF NOT EXISTS idx_chat_messages_sender
    ON chat_messages(sender_id);

-- =====================================================
-- MEDIUM PRIORITY: Other missing FK/query indexes
-- =====================================================

-- DriverReviewRepository.findByOrderIdAndCustomerId
CREATE INDEX IF NOT EXISTS idx_driver_reviews_customer
    ON driver_reviews(customer_id);

-- WebhookDlqRepository.findByProviderEventId
CREATE INDEX IF NOT EXISTS idx_webhook_dlq_provider_event
    ON webhook_dlq(provider_event_id);

-- FK order_item_modifiers.modifier_id -> modifiers(id)
CREATE INDEX IF NOT EXISTS idx_order_item_modifiers_modifier
    ON order_item_modifiers(modifier_id);

-- PasswordResetTokenRepository.deleteByExpiresAtBefore
CREATE INDEX IF NOT EXISTS idx_prt_expires
    ON password_reset_tokens(expires_at);

-- =====================================================
-- LOW PRIORITY: Optimize filtered/sorted queries
-- =====================================================

-- VendorRepository.findByMinimumRating (WHERE rating >= ? AND active)
CREATE INDEX IF NOT EXISTS idx_vendor_rating
    ON vendors(rating) WHERE active = TRUE;

-- PaymentRepository date-range revenue queries
CREATE INDEX IF NOT EXISTS idx_payment_created
    ON payments(created_at);

-- ReviewRepository: nearly all queries filter hidden = false
CREATE INDEX IF NOT EXISTS idx_review_vendor_hidden
    ON reviews(vendor_id, hidden) WHERE hidden = FALSE;

-- DriverReviewRepository: all queries filter hidden = false
CREATE INDEX IF NOT EXISTS idx_driver_reviews_driver_hidden
    ON driver_reviews(driver_id, hidden) WHERE hidden = FALSE;
