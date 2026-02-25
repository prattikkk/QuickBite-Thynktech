package com.quickbite.vendors.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for inventory stock update requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryUpdateDTO {

    @NotNull
    @Min(0)
    private Integer stockCount;

    @Min(0)
    private Integer lowStockThreshold;

    private Boolean autoDisableOnZero;
}
