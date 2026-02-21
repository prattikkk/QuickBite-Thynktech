package com.quickbite.promotions.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from validating a promo code against a subtotal.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoValidateResponse {
    private boolean valid;
    private String code;
    private String description;
    private String discountType;
    private Long discountCents;   // computed discount amount for the given subtotal
    private String message;       // human-readable validation message
}
