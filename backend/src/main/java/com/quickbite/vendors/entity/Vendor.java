package com.quickbite.vendors.entity;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vendor entity representing restaurants/food vendors.
 * Each vendor is linked to a user account.
 */
@Entity
@Table(name = "vendors", indexes = {
    @Index(name = "idx_vendor_name", columnList = "name"),
    @Index(name = "idx_vendor_user", columnList = "user_id"),
    @Index(name = "idx_vendor_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "open_hours", columnDefinition = "jsonb")
    private Map<String, String> openHours;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MenuItem> menuItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;

    // Helper method to add menu item
    public void addMenuItem(MenuItem menuItem) {
        menuItems.add(menuItem);
        menuItem.setVendor(this);
    }

    // Helper method to remove menu item
    public void removeMenuItem(MenuItem menuItem) {
        menuItems.remove(menuItem);
        menuItem.setVendor(null);
    }
}
