package com.quickbite.reviews.entity;

import com.quickbite.orders.entity.Order;
import com.quickbite.users.entity.User;
import com.quickbite.vendors.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Review entity — customer rates a vendor after an order is delivered.
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_vendor", columnList = "vendor_id"),
    @Index(name = "idx_review_customer", columnList = "customer_id"),
    @Index(name = "idx_review_order", columnList = "order_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_review_order_customer", columnNames = {"order_id", "customer_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(columnDefinition = "text")
    private String comment;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hidden = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;
}
