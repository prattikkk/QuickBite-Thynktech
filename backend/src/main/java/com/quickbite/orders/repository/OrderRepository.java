package com.quickbite.orders.repository;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Order entity operations.
 * Provides order search, filtering by customer, vendor, driver, and status.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    /**
     * Find orders by customer ID.
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return Page of orders
     */
    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * Find orders by vendor ID.
     *
     * @param vendorId the vendor ID
     * @param pageable pagination information
     * @return Page of orders
     */
    Page<Order> findByVendorId(UUID vendorId, Pageable pageable);

    /**
     * Find orders by driver ID.
     *
     * @param driverId the driver ID
     * @param pageable pagination information
     * @return Page of orders
     */
    Page<Order> findByDriverId(UUID driverId, Pageable pageable);

    /**
     * Find orders by status.
     *
     * @param status the order status
     * @param pageable pagination information
     * @return Page of orders with the specified status
     */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /**
     * Find orders by vendor ID and status.
     *
     * @param vendorId the vendor ID
     * @param status the order status
     * @return List of orders
     */
    List<Order> findByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    /**
     * Find orders by customer ID and status.
     *
     * @param customerId the customer ID
     * @param status the order status
     * @param pageable pagination information
     * @return Page of orders
     */
    Page<Order> findByCustomerIdAndStatus(UUID customerId, OrderStatus status, Pageable pageable);

    /**
     * Find orders assigned to a driver with specific status.
     *
     * @param driverId the driver ID
     * @param status the order status
     * @return List of orders
     */
    List<Order> findByDriverIdAndStatus(UUID driverId, OrderStatus status);

    /**
     * Find unassigned orders (driver is null) with specific status.
     *
     * @param status the order status
     * @return List of unassigned orders
     */
    List<Order> findByDriverIsNullAndStatus(OrderStatus status);

    /**
     * Find orders created within a date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return Page of orders
     */
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate ORDER BY o.createdAt DESC")
    Page<Order> findOrdersInDateRange(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        Pageable pageable
    );

    /**
     * Find orders by vendor and date range.
     *
     * @param vendorId the vendor ID
     * @param startDate start date
     * @param endDate end date
     * @return List of orders
     */
    @Query("SELECT o FROM Order o WHERE o.vendor.id = :vendorId " +
           "AND o.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY o.createdAt DESC")
    List<Order> findVendorOrdersInDateRange(
        @Param("vendorId") UUID vendorId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Count orders by customer ID.
     *
     * @param customerId the customer ID
     * @return count of orders
     */
    Long countByCustomerId(UUID customerId);

    /**
     * Count orders by vendor ID and status.
     *
     * @param vendorId the vendor ID
     * @param status the order status
     * @return count of orders
     */
    Long countByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    /**
     * Calculate total revenue for a vendor in date range.
     *
     * @param vendorId the vendor ID
     * @param startDate start date
     * @param endDate end date
     * @return total revenue in cents
     */
    @Query("SELECT COALESCE(SUM(o.totalCents), 0) FROM Order o " +
           "WHERE o.vendor.id = :vendorId " +
           "AND o.status IN ('COMPLETED', 'DELIVERED') " +
           "AND o.createdAt BETWEEN :startDate AND :endDate")
    Long calculateVendorRevenue(
        @Param("vendorId") UUID vendorId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate
    );

    // ── Fraud velocity queries ──

    /**
     * Count orders by customer created after a given time.
     */
    long countByCustomerIdAndCreatedAtAfter(UUID customerId, OffsetDateTime after);

    /**
     * Sum total cents spent by a customer after a given time.
     */
    @Query("SELECT COALESCE(SUM(o.totalCents), 0) FROM Order o " +
           "WHERE o.customer.id = :customerId AND o.createdAt > :after")
    Long sumTotalCentsByCustomerIdAndCreatedAtAfter(
        @Param("customerId") UUID customerId,
        @Param("after") OffsetDateTime after
    );

    /**
     * Count orders by customer, status, and created after a given time.
     */
    long countByCustomerIdAndStatusAndCreatedAtAfter(
        UUID customerId, OrderStatus status, OffsetDateTime after
    );

    // ── Data retention queries ──

    /**
     * Count orders older than a given date.
     */
    long countByCreatedAtBefore(OffsetDateTime before);
}
