package com.quickbite.analytics.controller;

import com.quickbite.analytics.service.VendorReportService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vendor-specific analytics.
 * Vendors can view their own reports; admins can view any vendor's reports.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Vendor Reports", description = "Vendor-specific reporting and analytics")
@SecurityRequirement(name = "bearerAuth")
public class VendorReportController {

    private final VendorReportService vendorReportService;

    @GetMapping("/api/vendors/{vendorId}/reports/kpis")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Get vendor KPIs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVendorKpis(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "weekly") String period) {
        log.debug("Getting KPIs for vendor {} period {}", vendorId, period);
        Map<String, Object> kpis = vendorReportService.getVendorKpis(vendorId, period);
        return ResponseEntity.ok(ApiResponse.success("Vendor KPIs retrieved", kpis));
    }

    @GetMapping("/api/vendors/{vendorId}/reports/revenue")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Get vendor revenue breakdown")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getVendorRevenue(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "weekly") String period) {
        log.debug("Getting revenue breakdown for vendor {} period {}", vendorId, period);
        List<Map<String, Object>> revenue = vendorReportService.getVendorRevenueBreakdown(vendorId, period);
        return ResponseEntity.ok(ApiResponse.success("Vendor revenue retrieved", revenue));
    }

    @GetMapping("/api/vendors/{vendorId}/reports/export")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Export vendor analytics CSV")
    public ResponseEntity<byte[]> exportVendorReport(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "monthly") String period) {
        log.debug("Exporting CSV for vendor {} period {}", vendorId, period);
        byte[] csvBytes = vendorReportService.exportVendorCsv(vendorId, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=vendor-report-" + period + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
