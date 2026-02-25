package com.quickbite.vendors.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Vendor commission configuration.
 * Supports tiered commission with per-vendor rates and flat fees.
 */
@Entity
@Table(name = "vendor_commissions", indexes = {
    @Index(name = "idx_vendor_commission_vendor", columnList = "vendor_id"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    /** Commission rate in basis points (1500 = 15%). */
    @Column(name = "commission_rate_bps", nullable = false)
    @Builder.Default
    private Integer commissionRateBps = 1500;

    /** Flat fee per order in cents. */
    @Column(name = "flat_fee_cents", nullable = false)
    @Builder.Default
    private Long flatFeeCents = 0L;

    @Column(name = "effective_from", nullable = false, columnDefinition = "timestamptz")
    @Builder.Default
    private OffsetDateTime effectiveFrom = OffsetDateTime.now();

    @Column(name = "effective_until", columnDefinition = "timestamptz")
    private OffsetDateTime effectiveUntil;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    /**
     * Calculate commission amount in cents for a given order subtotal.
     */
    public long calculateCommission(long subtotalCents) {
        long percentCommission = subtotalCents * commissionRateBps / 10_000;
        return percentCommission + flatFeeCents;
    }
}
