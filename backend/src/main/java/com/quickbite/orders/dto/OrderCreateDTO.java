package com.quickbite.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for creating a new order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDTO {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemDTO> items;

    @NotNull(message = "Delivery address is required")
    private UUID addressId;

    private Instant scheduledTime;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String specialInstructions;

    /** Optional promo code to apply discount. */
    private String promoCode;

    /** PICKUP or DELIVERY (defaults to DELIVERY if null). */
    private String deliveryType;

    public enum PaymentMethod {
        CARD,
        UPI,
        CASH_ON_DELIVERY
    }
}
