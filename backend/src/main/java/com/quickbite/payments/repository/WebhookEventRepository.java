package com.quickbite.payments.repository;

import com.quickbite.payments.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * Find unprocessed events that are due for retry.
     */
    @Query("SELECT we FROM WebhookEvent we " +
           "WHERE we.processed = false AND we.attempts < we.maxAttempts " +
           "AND (we.nextRetryAt IS NULL OR we.nextRetryAt <= :now) " +
           "ORDER BY we.createdAt ASC")
    List<WebhookEvent> findPendingRetries(OffsetDateTime now);

    /**
     * Count unprocessed events (for monitoring).
     */
    long countByProcessedFalse();
}
