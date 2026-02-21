package com.quickbite.promotions.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for creating/updating a promo code (admin).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoCreateRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private String description;

    @NotNull(message = "Discount type is required")
    private String discountType; // PERCENT or FIXED

    @NotNull(message = "Discount value is required")
    private Long discountValue;

    private Long minOrderCents;
    private Long maxDiscountCents;
    private Integer maxUses;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private Boolean active;
}
