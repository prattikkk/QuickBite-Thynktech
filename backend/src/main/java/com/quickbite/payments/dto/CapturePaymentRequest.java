package com.quickbite.payments.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for capturing a payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CapturePaymentRequest {

    @NotBlank(message = "Provider payment ID is required")
    private String providerPaymentId;
    
    private Long amountCents; // optional, capture partial amount
}
