package com.quickbite.orders.entity;

import com.quickbite.payments.entity.Payment;
import com.quickbite.payments.entity.PaymentMethod;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.users.entity.Address;
import com.quickbite.users.entity.User;
import com.quickbite.vendors.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order entity representing customer orders.
 * Tracks order lifecycle from placement to delivery.
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_customer", columnList = "customer_id"),
    @Index(name = "idx_order_vendor", columnList = "vendor_id"),
    @Index(name = "idx_order_driver", columnList = "driver_id"),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_created", columnList = "created_at"),
    @Index(name = "idx_order_customer_status", columnList = "customer_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "order_number", unique = true, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private User driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_address_id")
    private Address deliveryAddress;

    @Column(name = "subtotal_cents", nullable = false)
    private Long subtotalCents;

    @Column(name = "delivery_fee_cents", nullable = false)
    private Long deliveryFeeCents;

    @Column(name = "tax_cents", nullable = false)
    private Long taxCents;

    @Column(name = "total_cents", nullable = false)
    private Long totalCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private OrderStatus status = OrderStatus.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 50)
    private PaymentStatus paymentStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(name = "scheduled_time", columnDefinition = "timestamptz")
    private OffsetDateTime scheduledTime;

    @Column(name = "delivered_at", columnDefinition = "timestamptz")
    private OffsetDateTime deliveredAt;

    @Column(name = "cancelled_at", columnDefinition = "timestamptz")
    private OffsetDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    @Column(name = "special_instructions", columnDefinition = "text")
    private String specialInstructions;

    // ---- Phase 3: Growth fields ----

    @Column(name = "promo_code", length = 50)
    private String promoCode;

    @Column(name = "discount_cents", nullable = false)
    @Builder.Default
    private Long discountCents = 0L;

    @Column(name = "estimated_delivery_at", columnDefinition = "timestamptz")
    private OffsetDateTime estimatedDeliveryAt;

    @Column(name = "estimated_prep_mins")
    private Integer estimatedPrepMins;

    @Column(name = "delivery_proof_id", columnDefinition = "uuid")
    private UUID deliveryProofId;

    // ---- end Phase 3 ----

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    // Helper methods
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }

    // Alias method for OrderService compatibility
    public List<OrderItem> getItems() {
        return orderItems;
    }

    public void setItems(List<OrderItem> items) {
        this.orderItems = items;
    }
}
