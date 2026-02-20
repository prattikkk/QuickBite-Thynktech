package com.quickbite.vendors.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating a menu item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemCreateDTO {

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 1, message = "Price must be at least 1 cent")
    private Long priceCents;

    private Boolean available;

    private Integer prepTimeMins;

    private String category;

    private String imageUrl;
}
