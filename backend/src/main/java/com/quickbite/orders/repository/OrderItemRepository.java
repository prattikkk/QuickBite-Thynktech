package com.quickbite.orders.repository;

import com.quickbite.orders.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for OrderItem entity operations.
 * Manages line items within orders.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    /**
     * Find all items for an order.
     *
     * @param orderId the order ID
     * @return List of order items
     */
    List<OrderItem> findByOrderId(UUID orderId);

    /**
     * Find order items by menu item ID.
     *
     * @param menuItemId the menu item ID
     * @return List of order items
     */
    List<OrderItem> findByMenuItemId(UUID menuItemId);

    /**
     * Count items in an order.
     *
     * @param orderId the order ID
     * @return count of items
     */
    Long countByOrderId(UUID orderId);

    /**
     * Calculate total quantity for a menu item (for analytics).
     *
     * @param menuItemId the menu item ID
     * @return total quantity ordered
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.menuItem.id = :menuItemId")
    Long sumQuantityByMenuItemId(@Param("menuItemId") UUID menuItemId);

    /**
     * Find most ordered items for a vendor.
     *
     * @param vendorId the vendor ID
     * @param pageable pagination info (use PageRequest.of(0, limit))
     * @return List of menu item IDs ordered by popularity
     */
    @Query("SELECT oi.menuItem.id FROM OrderItem oi " +
           "WHERE oi.menuItem.vendor.id = :vendorId " +
           "GROUP BY oi.menuItem.id " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<UUID> findMostOrderedMenuItems(@Param("vendorId") UUID vendorId, Pageable pageable);

    /**
     * Delete all items for an order.
     *
     * @param orderId the order ID
     */
    void deleteByOrderId(UUID orderId);
}
