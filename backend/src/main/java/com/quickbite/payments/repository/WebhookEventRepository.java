package com.quickbite.payments.repository;

import com.quickbite.payments.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for WebhookEvent entity operations.
 */
@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, UUID> {

    /**
     * Check if webhook event has been processed already (idempotency).
     *
     * @param providerEventId provider's event ID
     * @return true if event exists
     */
    boolean existsByProviderEventId(String providerEventId);

    /**
     * Find webhook event by provider event ID.
     *
     * @param providerEventId provider's event ID
     * @return Optional<WebhookEvent>
     */
    Optional<WebhookEvent> findByProviderEventId(String providerEventId);
}
