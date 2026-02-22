package com.quickbite.delivery.entity;

import com.quickbite.users.entity.User;
import com.quickbite.orders.entity.Order;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Proof-of-delivery record — photo, OTP, or signature.
 * Phase 3 — Proof-of-Delivery & Notifications.
 */
@Entity
@Table(name = "delivery_proofs", indexes = {
    @Index(name = "idx_delivery_proofs_order", columnList = "order_id"),
    @Index(name = "idx_delivery_proofs_driver", columnList = "driver_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DeliveryProof {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 20)
    @Builder.Default
    private ProofType proofType = ProofType.PHOTO;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_verified")
    @Builder.Default
    private Boolean otpVerified = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(precision = 11, scale = 8)
    private BigDecimal lng;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private OffsetDateTime submittedAt = OffsetDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
