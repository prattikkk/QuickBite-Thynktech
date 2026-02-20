package com.quickbite.orders.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating order status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateDTO {

    @NotBlank(message = "Status is required")
    private String status;

    private String note;
    
    // Location for delivery tracking (optional)
    private Double locationLat;
    private Double locationLng;
}
