package com.quickbite.vendors.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.vendors.dto.VendorResponseDTO;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for vendor browsing endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor browsing endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorRepository vendorRepository;

    /**
     * List all active vendors.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "List vendors", description = "List all active vendors")
    public ResponseEntity<ApiResponse<List<VendorResponseDTO>>> listVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Listing active vendors, page={}, size={}", page, size);

        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var vendors = vendorRepository.findAll(pageable)
                .stream()
                .filter(v -> Boolean.TRUE.equals(v.getActive()))
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully", vendors));
    }

    /**
     * Get vendor by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get vendor", description = "Get vendor details by ID")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> getVendor(@PathVariable UUID id) {
        log.debug("Getting vendor: {}", id);

        var vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));

        return ResponseEntity.ok(ApiResponse.success("Vendor retrieved successfully", toDTO(vendor)));
    }

    /**
     * Search vendors by name.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Search vendors", description = "Search vendors by name")
    public ResponseEntity<ApiResponse<List<VendorResponseDTO>>> searchVendors(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.debug("Searching vendors with query: {}", query);

        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        var vendors = vendorRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", vendors));
    }

    private VendorResponseDTO toDTO(Vendor vendor) {
        return VendorResponseDTO.builder()
                .id(vendor.getId())
                .userId(vendor.getUser() != null ? vendor.getUser().getId() : null)
                .name(vendor.getName())
                .description(vendor.getDescription())
                .address(vendor.getAddress())
                .lat(vendor.getLat())
                .lng(vendor.getLng())
                .openHours(vendor.getOpenHours())
                .rating(vendor.getRating())
                .active(vendor.getActive())
                .menuItemCount(vendor.getMenuItems() != null ? vendor.getMenuItems().size() : 0)
                .createdAt(vendor.getCreatedAt())
                .updatedAt(vendor.getUpdatedAt())
                .build();
    }
}
