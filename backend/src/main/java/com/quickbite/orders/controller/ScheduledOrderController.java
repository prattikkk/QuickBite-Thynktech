package com.quickbite.orders.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.dto.ScheduleValidationDTO;
import com.quickbite.orders.service.ScheduledOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for scheduled order endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Scheduled Orders", description = "Scheduled order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ScheduledOrderController {

    private final ScheduledOrderService scheduledOrderService;

    /**
     * List upcoming scheduled orders for a vendor.
     */
    @GetMapping("/api/vendors/{vendorId}/scheduled-orders")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "List scheduled orders", description = "Get upcoming scheduled orders for a vendor")
    public ResponseEntity<ApiResponse<List<OrderResponseDTO>>> getScheduledOrders(
            @PathVariable UUID vendorId
    ) {
        log.debug("Getting scheduled orders for vendor {}", vendorId);
        List<OrderResponseDTO> orders = scheduledOrderService.getScheduledOrders(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled orders retrieved", orders));
    }

    /**
     * Validate a proposed scheduled time for an order.
     */
    @PostMapping("/api/orders/validate-schedule")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "Validate schedule time", description = "Check if a scheduled time is valid for a vendor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSchedule(
            @Valid @RequestBody ScheduleValidationDTO dto
    ) {
        log.debug("Validating schedule time {} for vendor {}", dto.getScheduledTime(), dto.getVendorId());
        boolean valid = scheduledOrderService.validateScheduledTime(dto.getScheduledTime(), dto.getVendorId());

        String message = valid
                ? "Scheduled time is valid"
                : "Scheduled time is invalid. Must be at least 30 minutes in the future and within vendor operating hours.";

        Map<String, Object> result = scheduledOrderService.buildValidationResult(valid, message);
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
}
