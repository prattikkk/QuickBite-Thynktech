package com.quickbite.vendors.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MenuItem entity representing food items offered by vendors.
 * Prices stored in cents/paise to avoid floating point issues.
 */
@Entity
@Table(name = "menu_items", indexes = {
    @Index(name = "idx_menuitem_vendor", columnList = "vendor_id"),
    @Index(name = "idx_menuitem_available", columnList = "available"),
    @Index(name = "idx_menuitem_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "prep_time_mins")
    @Builder.Default
    private Integer prepTimeMins = 15;

    @Column(length = 100)
    private String category;

    @Column(name = "image_url", columnDefinition = "text")
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;
}
