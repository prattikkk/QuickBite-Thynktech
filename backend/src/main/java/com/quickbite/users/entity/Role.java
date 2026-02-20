package com.quickbite.users.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Role entity representing user roles in the system.
 * Roles: ADMIN, VENDOR, CUSTOMER, DRIVER
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    // Role constants
    public static final String ADMIN = "ADMIN";
    public static final String VENDOR = "VENDOR";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DRIVER = "DRIVER";
}
