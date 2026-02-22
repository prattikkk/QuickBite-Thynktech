package com.quickbite.orders.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for a single driver GPS location sample.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverLocationDTO {
    private UUID id;
    private UUID driverId;
    private double lat;
    private double lng;
    private Double accuracy;
    private Double speed;
    private Double heading;
    private OffsetDateTime recordedAt;
}
