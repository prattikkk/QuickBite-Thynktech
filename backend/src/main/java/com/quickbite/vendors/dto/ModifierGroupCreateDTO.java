package com.quickbite.vendors.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a modifier group.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierGroupCreateDTO {

    @NotBlank
    private String name;

    private Boolean required;

    @Min(0)
    private Integer minSelections;

    @Min(1)
    private Integer maxSelections;
}
