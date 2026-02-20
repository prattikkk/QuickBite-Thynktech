package com.quickbite.delivery.repository;

import com.quickbite.delivery.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for DeliveryStatus entity operations.
 * Tracks order status changes and delivery progress.
 */
@Repository
public interface DeliveryStatusRepository extends JpaRepository<DeliveryStatus, UUID> {

    /**
     * Find all status changes for an order, ordered by time.
     *
     * @param orderId the order ID
     * @return List of delivery status entries
     */
    @Query("SELECT ds FROM DeliveryStatus ds WHERE ds.order.id = :orderId ORDER BY ds.changedAt ASC")
    List<DeliveryStatus> findByOrderIdOrderByChangedAtAsc(@Param("orderId") UUID orderId);

    /**
     * Find the latest status for an order.
     *
     * @param orderId the order ID
     * @return Optional containing the latest status
     */
    @Query("SELECT ds FROM DeliveryStatus ds WHERE ds.order.id = :orderId ORDER BY ds.changedAt DESC LIMIT 1")
    Optional<DeliveryStatus> findLatestStatusByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find status changes by order and status value.
     *
     * @param orderId the order ID
     * @param status the status value
     * @return List of matching status entries
     */
    List<DeliveryStatus> findByOrderIdAndStatus(UUID orderId, String status);

    /**
     * Find all status changes made by a user (e.g., vendor or driver).
     *
     * @param userId the user ID who made the change
     * @return List of status changes
     */
    List<DeliveryStatus> findByChangedByUserId(UUID userId);

    /**
     * Count status changes for an order.
     *
     * @param orderId the order ID
     * @return count of status changes
     */
    Long countByOrderId(UUID orderId);

    /**
     * Find status entries with location data (for driver tracking).
     *
     * @param orderId the order ID
     * @return List of status entries with location
     */
    @Query("SELECT ds FROM DeliveryStatus ds WHERE ds.order.id = :orderId " +
           "AND ds.locationLat IS NOT NULL AND ds.locationLng IS NOT NULL " +
           "ORDER BY ds.changedAt DESC")
    List<DeliveryStatus> findDeliveryLocations(@Param("orderId") UUID orderId);
}
