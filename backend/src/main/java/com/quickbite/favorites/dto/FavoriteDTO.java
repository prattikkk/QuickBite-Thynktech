package com.quickbite.favorites.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for favorite vendor response.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class FavoriteDTO {
    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private String vendorDescription;
    private String vendorAddress;
    private Double rating;
    private Boolean vendorActive;
    private OffsetDateTime createdAt;
}
