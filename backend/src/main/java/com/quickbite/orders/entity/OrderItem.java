package com.quickbite.orders.entity;

import com.quickbite.vendors.entity.MenuItem;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * OrderItem entity representing line items within an order.
 * Captures menu item details at the time of order (price snapshot).
 */
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_orderitem_order", columnList = "order_id"),
    @Index(name = "idx_orderitem_menuitem", columnList = "menu_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "special_instructions", columnDefinition = "text")
    private String specialInstructions;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Calculate total for this line item
     */
    public Long calculateTotal() {
        return priceCents * quantity;
    }
}
