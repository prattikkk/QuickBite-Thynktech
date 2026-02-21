package com.quickbite.favorites.entity;

import com.quickbite.users.entity.User;
import com.quickbite.vendors.entity.Vendor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Favorite entity — a customer's bookmarked vendor.
 */
@Entity
@Table(name = "favorites", uniqueConstraints = {
    @UniqueConstraint(name = "uq_favorite_user_vendor", columnNames = {"user_id", "vendor_id"})
}, indexes = {
    @Index(name = "idx_favorite_user",   columnList = "user_id"),
    @Index(name = "idx_favorite_vendor", columnList = "vendor_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;
}
