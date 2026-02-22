package com.quickbite.orders.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for driver profile data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverProfileDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private String email;
    private String vehicleType;
    private String licensePlate;
    private Boolean isOnline;
    private Double currentLat;
    private Double currentLng;
    private Integer totalDeliveries;
    private BigDecimal successRate;
    private OffsetDateTime shiftStartedAt;
    private OffsetDateTime shiftEndedAt;
}
