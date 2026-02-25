package com.quickbite.vendors.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for vendor response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorResponseDTO {
    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private Map<String, String> openHours;
    private BigDecimal rating;
    private long reviewCount;
    private BigDecimal deliveryRadiusKm;
    private Boolean active;
    private int menuItemCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
