package com.quickbite.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReviewDTO {
    private UUID id;
    private UUID orderId;
    private UUID vendorId;
    private UUID customerId;
    private String customerName;
    private Integer rating;
    private String comment;
    private OffsetDateTime createdAt;
}
