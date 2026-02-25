package com.quickbite.vendors.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.vendors.dto.InventoryUpdateDTO;
import com.quickbite.vendors.dto.MenuItemResponseDTO;
import com.quickbite.vendors.service.InventoryService;
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
 * REST controller for vendor inventory management.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
@Tag(name = "Inventory", description = "Vendor inventory management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Get inventory status for all items of a vendor.
     */
    @GetMapping("/api/vendors/{vendorId}/inventory")
    @Operation(summary = "Get inventory status", description = "List inventory status for all vendor menu items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getInventoryStatus(
            @PathVariable UUID vendorId
    ) {
        log.debug("Getting inventory status for vendor {}", vendorId);
        List<Map<String, Object>> status = inventoryService.getInventoryStatus(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Inventory status retrieved", status));
    }

    /**
     * Update stock for a specific menu item.
     */
    @PutMapping("/api/vendors/{vendorId}/inventory/{itemId}")
    @Operation(summary = "Update item stock", description = "Update stock count and thresholds for a menu item")
    public ResponseEntity<ApiResponse<MenuItemResponseDTO>> updateStock(
            @PathVariable UUID vendorId,
            @PathVariable UUID itemId,
            @Valid @RequestBody InventoryUpdateDTO dto
    ) {
        log.debug("Updating stock for item {} vendor {}: count={}", itemId, vendorId, dto.getStockCount());

        if (dto.getLowStockThreshold() != null) {
            inventoryService.setLowStockThreshold(itemId, dto.getLowStockThreshold());
        }
        if (dto.getAutoDisableOnZero() != null) {
            inventoryService.setAutoDisableOnZero(itemId, dto.getAutoDisableOnZero());
        }

        MenuItemResponseDTO updated = inventoryService.updateStock(itemId, dto.getStockCount());
        return ResponseEntity.ok(ApiResponse.success("Stock updated", updated));
    }

    /**
     * Reset daily stock for all items of a vendor.
     */
    @PostMapping("/api/vendors/{vendorId}/inventory/reset-daily")
    @Operation(summary = "Reset daily stock", description = "Reset stock counts to daily defaults for all vendor items")
    public ResponseEntity<ApiResponse<Void>> resetDailyStock(
            @PathVariable UUID vendorId
    ) {
        log.debug("Resetting daily stock for vendor {}", vendorId);
        inventoryService.resetDailyStock(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Daily stock reset completed", null));
    }
}
