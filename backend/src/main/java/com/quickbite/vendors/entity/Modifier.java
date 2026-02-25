package com.quickbite.vendors.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Modifier entity representing an individual modifier option within a group.
 * E.g. "Large" (+$2.00), "Extra Cheese" (+$1.50).
 */
@Entity
@Table(name = "modifiers", indexes = {
    @Index(name = "idx_modifier_group", columnList = "group_id"),
    @Index(name = "idx_modifier_sort", columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Modifier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private ModifierGroup group;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "price_cents", nullable = false)
    @Builder.Default
    private Long priceCents = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;
}
