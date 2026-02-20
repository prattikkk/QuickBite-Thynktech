package com.quickbite.delivery.entity;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DeliveryStatus entity tracking order status changes and location.
 * Provides audit trail for order lifecycle events.
 */
@Entity
@Table(name = "delivery_status", indexes = {
    @Index(name = "idx_deliverystatus_order", columnList = "order_id"),
    @Index(name = "idx_deliverystatus_changed_at", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OrderStatus status;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(columnDefinition = "text")
    private String note;

    @CreationTimestamp
    @Column(name = "changed_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "location_lat", precision = 10, scale = 7)
    private BigDecimal locationLat;

    @Column(name = "location_lng", precision = 10, scale = 7)
    private BigDecimal locationLng;
}
