package com.quickbite.payments.entity;

import com.quickbite.orders.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payment entity tracking payment transactions for orders.
 * Integrates with external payment providers like Razorpay.
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_provider_id", columnList = "provider_payment_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "amount_cents", nullable = false)
    private Long amountCents;

    @Column(length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(length = 50)
    private String provider;

    @Column(name = "provider_payment_id", length = 255)
    private String providerPaymentId;

    /** Stripe PaymentIntent client_secret — returned to frontend to confirm payment. */
    @Column(name = "client_secret", columnDefinition = "text")
    private String clientSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at", columnDefinition = "timestamptz")
    private OffsetDateTime paidAt;

    @Column(name = "failed_at", columnDefinition = "timestamptz")
    private OffsetDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;
}
