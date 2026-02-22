package com.quickbite.delivery.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for delivery proof records.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryProofResponseDTO {
    private UUID id;
    private UUID orderId;
    private UUID driverId;
    private String proofType;
    private String photoUrl;
    private Boolean otpVerified;
    private String notes;
    private BigDecimal lat;
    private BigDecimal lng;
    private OffsetDateTime submittedAt;
    private OffsetDateTime createdAt;
}
