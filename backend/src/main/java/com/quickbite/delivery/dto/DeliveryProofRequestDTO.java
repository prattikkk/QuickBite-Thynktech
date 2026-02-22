package com.quickbite.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for submitting delivery proof (photo upload).
 * The actual photo file comes as a multipart part, not in this DTO.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DeliveryProofRequestDTO {

    private String notes;

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal lat;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal lng;
}
