package com.quickbite.analytics.controller;

import com.quickbite.analytics.service.VendorAnalyticsService;
import com.quickbite.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vendor analytics endpoints.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Vendor Analytics", description = "Vendor analytics and reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VendorAnalyticsController {

    private final VendorAnalyticsService vendorAnalyticsService;

    /**
     * Get analytics data for a vendor.
     */
    @GetMapping("/api/vendors/{vendorId}/analytics")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Get vendor analytics", description = "Retrieve analytics for a vendor over a given period")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVendorAnalytics(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "weekly") String period
    ) {
        log.debug("Getting analytics for vendor {} period {}", vendorId, period);
        Map<String, Object> analytics = vendorAnalyticsService.getVendorAnalytics(vendorId, period);
        return ResponseEntity.ok(ApiResponse.success("Vendor analytics retrieved", analytics));
    }

    /**
     * Export vendor analytics as CSV.
     */
    @GetMapping("/api/vendors/{vendorId}/analytics/export")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Export vendor analytics CSV", description = "Export vendor analytics as a CSV file")
    public ResponseEntity<byte[]> exportVendorAnalytics(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "monthly") String period
    ) {
        log.debug("Exporting analytics CSV for vendor {} period {}", vendorId, period);
        byte[] csvBytes = vendorAnalyticsService.exportAnalyticsCsv(vendorId, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vendor-analytics-" + vendorId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
