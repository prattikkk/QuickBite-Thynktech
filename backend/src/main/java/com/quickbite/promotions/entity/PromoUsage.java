package com.quickbite.promotions.entity;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks per-user promo code usage for enforcing maxUsesPerUser limits.
 */
@Entity
@Table(name = "promo_usage", indexes = {
    @Index(name = "idx_promo_usage_user", columnList = "user_id"),
    @Index(name = "idx_promo_usage_promo", columnList = "promo_id"),
    @Index(name = "idx_promo_usage_user_promo", columnList = "user_id, promo_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "promo_id", nullable = false)
    private UUID promoId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "used_at", nullable = false, columnDefinition = "timestamptz")
    @Builder.Default
    private OffsetDateTime usedAt = OffsetDateTime.now();
}
