package com.quickbite.vendors.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.vendors.entity.VendorCommission;
import com.quickbite.vendors.service.VendorCommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Admin endpoints for managing vendor commission rates.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/commissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Commissions", description = "Vendor commission management")
@SecurityRequirement(name = "bearerAuth")
public class AdminCommissionController {

    private final VendorCommissionService vendorCommissionService;

    @GetMapping("/{vendorId}")
    @Operation(summary = "Get commission rate for a vendor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommission(@PathVariable UUID vendorId) {
        Map<String, Object> rate = vendorCommissionService.getCommissionRate(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Commission rate retrieved", rate));
    }

    @PutMapping("/{vendorId}")
    @Operation(summary = "Set commission rate for a vendor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setCommission(
            @PathVariable UUID vendorId,
            @RequestBody Map<String, Object> body) {
        int rateBps = body.containsKey("commissionRateBps")
                ? ((Number) body.get("commissionRateBps")).intValue() : 1500;
        long flatFeeCents = body.containsKey("flatFeeCents")
                ? ((Number) body.get("flatFeeCents")).longValue() : 0L;

        vendorCommissionService.setCommission(vendorId, rateBps, flatFeeCents);
        log.info("Commission set for vendor {}: {}bps + {}c", vendorId, rateBps, flatFeeCents);

        Map<String, Object> result = vendorCommissionService.getCommissionRate(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Commission rate updated", result));
    }
}
