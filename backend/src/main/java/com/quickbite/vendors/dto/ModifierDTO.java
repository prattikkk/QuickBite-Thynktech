package com.quickbite.vendors.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for individual modifier responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierDTO {
    private UUID id;
    private UUID groupId;
    private String name;
    private Long priceCents;
    private Boolean available;
    private Integer sortOrder;
}
