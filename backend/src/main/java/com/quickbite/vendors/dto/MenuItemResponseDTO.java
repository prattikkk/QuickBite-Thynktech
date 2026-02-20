package com.quickbite.vendors.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for menu item response.
 * Price exposed as cents and as a decimal for convenience.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponseDTO {
    private UUID id;
    private UUID vendorId;
    private String name;
    private String description;
    private Long priceCents;
    private Double price;   // convenience: priceCents / 100.0
    private Boolean available;
    private Integer prepTimeMins;
    private String category;
    private String imageUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
