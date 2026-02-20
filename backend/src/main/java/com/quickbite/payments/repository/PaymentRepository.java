package com.quickbite.payments.repository;

import com.quickbite.payments.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Payment entity operations.
 * Manages payment transactions and provider integrations.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by order ID.
     *
     * @param orderId the order ID
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByOrderId(UUID orderId);

    /**
     * Find payment by provider payment ID.
     *
     * @param providerPaymentId the provider's payment ID
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    /**
     * Find payments by status.
     *
     * @param status the payment status
     * @param pageable pagination information
     * @return Page of payments with the specified status
     */
    Page<Payment> findByStatus(String status, Pageable pageable);

    /**
     * Find payments by order's customer ID.
     *
     * @param customerId the customer ID
     * @param pageable pagination information
     * @return Page of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.order.customer.id = :customerId ORDER BY p.createdAt DESC")
    Page<Payment> findPaymentsByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);

    /**
     * Find payments by order's vendor ID.
     *
     * @param vendorId the vendor ID
     * @param pageable pagination information
     * @return Page of payments
     */
    @Query("SELECT p FROM Payment p WHERE p.order.vendor.id = :vendorId ORDER BY p.createdAt DESC")
    Page<Payment> findPaymentsByVendorId(@Param("vendorId") UUID vendorId, Pageable pageable);

    /**
     * Find successful payments in date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @return List of successful payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCESS' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate " +
           "ORDER BY p.paidAt DESC")
    List<Payment> findSuccessfulPaymentsInDateRange(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Find failed payments for retry.
     *
     * @param maxRetryDate payments failed before this date
     * @param pageable pagination information
     * @return Page of failed payments
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' " +
           "AND p.failedAt < :maxRetryDate " +
           "ORDER BY p.createdAt ASC")
    Page<Payment> findFailedPaymentsForRetry(
        @Param("maxRetryDate") OffsetDateTime maxRetryDate,
        Pageable pageable
    );

    /**
     * Calculate total payment amount by vendor in date range.
     *
     * @param vendorId the vendor ID
     * @param startDate start date
     * @param endDate end date
     * @return total amount in cents
     */
    @Query("SELECT COALESCE(SUM(p.amountCents), 0) FROM Payment p " +
           "WHERE p.order.vendor.id = :vendorId " +
           "AND p.status = 'SUCCESS' " +
           "AND p.paidAt BETWEEN :startDate AND :endDate")
    Long calculateVendorPayments(
        @Param("vendorId") UUID vendorId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate
    );

    /**
     * Count payments by status.
     *
     * @param status the payment status
     * @return count of payments
     */
    Long countByStatus(String status);
}
