package com.quickbite.vendors.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for modifier group responses, including nested modifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierGroupDTO {
    private UUID id;
    private UUID menuItemId;
    private String name;
    private Boolean required;
    private Integer minSelections;
    private Integer maxSelections;
    private Integer sortOrder;
    private List<ModifierDTO> modifiers;
}
