package com.quickbite.users.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Address entity for user delivery addresses.
 * Supports multiple addresses per user with one default.
 */
@Entity
@Table(name = "addresses", indexes = {
    @Index(name = "idx_address_user", columnList = "user_id"),
    @Index(name = "idx_address_default", columnList = "user_id, is_default")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String line1;

    @Column(length = 255)
    private String line2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String postal;

    @Column(length = 100)
    @Builder.Default
    private String country = "India";

    @Column(precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt;
}
