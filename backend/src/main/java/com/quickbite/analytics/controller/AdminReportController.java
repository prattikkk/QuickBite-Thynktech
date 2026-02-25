package com.quickbite.analytics.controller;

import com.quickbite.analytics.service.AdminReportService;
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

import java.util.List;
import java.util.Map;

/**
 * REST controller for platform-wide admin reports.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Reports", description = "Platform-wide admin reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminReportController {

    private final AdminReportService adminReportService;

    /**
     * Get platform KPIs.
     */
    @GetMapping("/api/admin/reports/kpis")
    @Operation(summary = "Get platform KPIs", description = "Retrieve platform-wide key performance indicators")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformKpis(
            @RequestParam(defaultValue = "monthly") String period
    ) {
        log.debug("Getting platform KPIs for period {}", period);
        Map<String, Object> kpis = adminReportService.getPlatformKpis(period);
        return ResponseEntity.ok(ApiResponse.success("Platform KPIs retrieved", kpis));
    }

    /**
     * Get revenue breakdown report.
     */
    @GetMapping("/api/admin/reports/revenue")
    @Operation(summary = "Get revenue report", description = "Retrieve revenue breakdown by day")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRevenueReport(
            @RequestParam(defaultValue = "weekly") String period
    ) {
        log.debug("Getting revenue report for period {}", period);
        List<Map<String, Object>> revenue = adminReportService.getRevenueReport(period);
        return ResponseEntity.ok(ApiResponse.success("Revenue report retrieved", revenue));
    }

    /**
     * Get delivery time statistics.
     */
    @GetMapping("/api/admin/reports/delivery-times")
    @Operation(summary = "Get delivery time stats", description = "Retrieve delivery time statistics (avg, min, max, percentiles)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDeliveryTimeReport() {
        log.debug("Getting delivery time report");
        Map<String, Object> report = adminReportService.getDeliveryTimeReport();
        return ResponseEntity.ok(ApiResponse.success("Delivery time report retrieved", report));
    }

    /**
     * Export report as CSV.
     */
    @GetMapping("/api/admin/reports/export")
    @Operation(summary = "Export report CSV", description = "Export admin report data as CSV")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(defaultValue = "revenue") String type,
            @RequestParam(defaultValue = "monthly") String period
    ) {
        log.debug("Exporting {} report CSV for period {}", type, period);
        byte[] csvBytes = adminReportService.exportReportCsv(type, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-" + type + "-" + period + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
