package com.quickbite.promotions.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Promo code entity for discounts and coupons.
 */
@Entity
@Table(name = "promo_codes", indexes = {
    @Index(name = "idx_promo_code",   columnList = "code"),
    @Index(name = "idx_promo_active", columnList = "active")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** For FIXED: amount in cents. For PERCENT: basis points (1500 = 15%). */
    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    /** Minimum subtotal (in cents) required to apply this promo. */
    @Column(name = "min_order_cents", nullable = false)
    @Builder.Default
    private Long minOrderCents = 0L;

    /** Maximum discount in cents (caps PERCENT discounts). Null = no cap. */
    @Column(name = "max_discount_cents")
    private Long maxDiscountCents;

    /** Maximum number of times this promo can be redeemed. Null = unlimited. */
    @Column(name = "max_uses")
    private Integer maxUses;

    @Column(name = "current_uses", nullable = false)
    @Builder.Default
    private Integer currentUses = 0;

    @Column(name = "valid_from", columnDefinition = "timestamptz", nullable = false)
    @Builder.Default
    private OffsetDateTime validFrom = OffsetDateTime.now();

    @Column(name = "valid_until", columnDefinition = "timestamptz")
    private OffsetDateTime validUntil;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Maximum number of times a single user can use this promo. Null = unlimited. */
    @Column(name = "max_uses_per_user")
    private Integer maxUsesPerUser;

    /** BOGO: the free menu item ID given when this promo is applied. */
    @Column(name = "bogo_item_id", columnDefinition = "uuid")
    private UUID bogoItemId;

    /** If true, this promo can only be used on a customer's first order. */
    @Column(name = "first_order_only", nullable = false)
    @Builder.Default
    private Boolean firstOrderOnly = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    /**
     * Increment usage counter.
     */
    public void incrementUses() {
        this.currentUses++;
    }
}
