package com.quickbite.payments.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for payment intent response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    private UUID id;
    
    private String providerPaymentId;
    
    private String clientSecret;
    
    private Long amountCents;
    
    private String currency;
    
    private String status;
}
