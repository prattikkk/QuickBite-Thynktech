package com.quickbite.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for order item in response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {

    private UUID id;
    private UUID menuItemId;
    private String name;
    private Integer quantity;
    private Long unitPriceCents;
    private Long totalCents;
    private String specialInstructions;
}
