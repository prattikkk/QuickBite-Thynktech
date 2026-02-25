package com.quickbite.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DriverReviewDTO {
    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private UUID customerId;
    private String customerName;
    private UUID driverId;
    private String driverName;
    private Integer rating;
    private String comment;
    private Boolean disputed;
    private String disputeReason;
    private Boolean hidden;
    private OffsetDateTime createdAt;
}
