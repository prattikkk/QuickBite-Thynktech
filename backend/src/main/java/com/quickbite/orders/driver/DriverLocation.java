package com.quickbite.orders.driver;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * GPS sample point recorded while a driver is on-shift.
 * Stores recent location history for ETA estimation and audit.
 */
@Entity
@Table(name = "driver_locations", indexes = {
        @Index(name = "idx_driver_locations_driver_time", columnList = "driver_id, recorded_at DESC"),
        @Index(name = "idx_driver_locations_recorded", columnList = "recorded_at")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private User driver;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal lat;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal lng;

    /** Accuracy in metres (from Geolocation API). */
    private Double accuracy;

    /** Speed in m/s (from Geolocation API). */
    private Double speed;

    /** Heading in degrees (from Geolocation API). */
    private Double heading;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
