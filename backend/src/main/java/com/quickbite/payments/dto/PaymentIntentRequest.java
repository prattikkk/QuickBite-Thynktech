package com.quickbite.payments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for creating a payment intent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Currency is required")
    private String currency; // e.g., INR, USD

    private String paymentMethod; // CARD, UPI, CASH_ON_DELIVERY

    private String description;
}
