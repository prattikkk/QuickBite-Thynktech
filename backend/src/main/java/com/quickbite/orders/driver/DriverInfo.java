package com.quickbite.orders.driver;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Simple DTO for driver information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverInfo {
    private UUID driverId;
    private String name;
    private String phone;
    private Double lat;
    private Double lng;
    private Double distanceKm;
}
