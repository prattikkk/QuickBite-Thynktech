package com.quickbite.vendors.controller;

import com.quickbite.auth.security.CustomUserDetailsService;
import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.dto.MenuItemCreateDTO;
import com.quickbite.vendors.dto.MenuItemResponseDTO;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.MenuItemRepository;
import com.quickbite.vendors.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for menu item management.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Menu Items", description = "Menu item management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MenuItemController {

    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    /**
     * Get all menu items for a vendor.
     */
    @GetMapping("/api/vendors/{vendorId}/menu")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get vendor menu", description = "Get all available menu items for a vendor")
    public ResponseEntity<ApiResponse<List<MenuItemResponseDTO>>> getVendorMenu(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "false") boolean includeUnavailable
    ) {
        log.debug("Getting menu for vendor: {}", vendorId);

        List<MenuItem> items = includeUnavailable
                ? menuItemRepository.findByVendorId(vendorId)
                : menuItemRepository.findByVendorIdAndAvailableTrue(vendorId);

        var dtos = items.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Menu retrieved successfully", dtos));
    }

    /**
     * Get a specific menu item.
     */
    @GetMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get menu item", description = "Get a specific menu item by ID")
    public ResponseEntity<ApiResponse<MenuItemResponseDTO>> getMenuItem(@PathVariable UUID id) {
        var item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));
        return ResponseEntity.ok(ApiResponse.success("Menu item retrieved", toDTO(item)));
    }

    /**
     * Create a new menu item (VENDOR only, for their own vendor).
     */
    @PostMapping("/api/vendors/{vendorId}/menu")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Create menu item", description = "Add a new menu item to vendor's menu")
    public ResponseEntity<ApiResponse<MenuItemResponseDTO>> createMenuItem(
            @PathVariable UUID vendorId,
            @Valid @RequestBody MenuItemCreateDTO dto,
            Authentication authentication
    ) {
        log.info("Creating menu item for vendor: {}", vendorId);

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));

        MenuItem item = MenuItem.builder()
                .vendor(vendor)
                .name(dto.getName())
                .description(dto.getDescription())
                .priceCents(dto.getPriceCents())
                .available(dto.getAvailable() != null ? dto.getAvailable() : true)
                .prepTimeMins(dto.getPrepTimeMins() != null ? dto.getPrepTimeMins() : 15)
                .category(dto.getCategory())
                .imageUrl(dto.getImageUrl())
                .build();

        item = menuItemRepository.save(item);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Menu item created successfully", toDTO(item)));
    }

    /**
     * Update an existing menu item (VENDOR only).
     */
    @PutMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update menu item", description = "Update an existing menu item")
    public ResponseEntity<ApiResponse<MenuItemResponseDTO>> updateMenuItem(
            @PathVariable UUID id,
            @Valid @RequestBody MenuItemCreateDTO dto
    ) {
        log.info("Updating menu item: {}", id);

        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Menu item not found: " + id));

        if (dto.getName() != null) item.setName(dto.getName());
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getPriceCents() != null) item.setPriceCents(dto.getPriceCents());
        if (dto.getAvailable() != null) item.setAvailable(dto.getAvailable());
        if (dto.getPrepTimeMins() != null) item.setPrepTimeMins(dto.getPrepTimeMins());
        if (dto.getCategory() != null) item.setCategory(dto.getCategory());
        if (dto.getImageUrl() != null) item.setImageUrl(dto.getImageUrl());

        item = menuItemRepository.save(item);

        return ResponseEntity.ok(ApiResponse.success("Menu item updated successfully", toDTO(item)));
    }

    /**
     * Delete a menu item (VENDOR or ADMIN).
     */
    @DeleteMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete menu item", description = "Remove a menu item from the menu")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable UUID id) {
        log.info("Deleting menu item: {}", id);

        if (!menuItemRepository.existsById(id)) {
            throw new RuntimeException("Menu item not found: " + id);
        }

        menuItemRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Menu item deleted successfully", null));
    }

    private MenuItemResponseDTO toDTO(MenuItem item) {
        return MenuItemResponseDTO.builder()
                .id(item.getId())
                .vendorId(item.getVendor() != null ? item.getVendor().getId() : null)
                .name(item.getName())
                .description(item.getDescription())
                .priceCents(item.getPriceCents())
                .price(item.getPriceCents() != null ? item.getPriceCents() / 100.0 : null)
                .available(item.getAvailable())
                .prepTimeMins(item.getPrepTimeMins())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
