package com.quickbite.promotions.dto;

import com.quickbite.promotions.entity.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for promo code response.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class PromoCodeDTO {
    private UUID id;
    private String code;
    private String description;
    private DiscountType discountType;
    private Long discountValue;
    private Long minOrderCents;
    private Long maxDiscountCents;
    private Integer maxUses;
    private Integer currentUses;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private Boolean active;
    private OffsetDateTime createdAt;
}
