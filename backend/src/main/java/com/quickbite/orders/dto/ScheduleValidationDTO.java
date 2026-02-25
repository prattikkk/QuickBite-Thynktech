package com.quickbite.orders.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for validating a scheduled order time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleValidationDTO {

    @NotNull
    @Future
    private OffsetDateTime scheduledTime;

    @NotNull
    private UUID vendorId;
}
