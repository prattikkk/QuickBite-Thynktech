package com.quickbite.vendors.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating an individual modifier.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierCreateDTO {

    @NotBlank
    private String name;

    @Min(0)
    private Long priceCents;

    private Boolean available;
}
