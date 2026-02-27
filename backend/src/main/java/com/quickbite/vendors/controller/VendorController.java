package com.quickbite.vendors.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.dto.VendorCreateDTO;
import com.quickbite.vendors.dto.VendorResponseDTO;
import com.quickbite.vendors.dto.VendorUpdateDTO;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import com.quickbite.vendors.service.VendorCacheService;
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

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for vendor browsing + vendor profile management.
 */
@Slf4j
@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
@Tag(name = "Vendors", description = "Vendor browsing and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class VendorController {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;
    private final VendorCacheService vendorCacheService;

    // ── Browse (all authenticated users) ────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "List vendors", description = "List all active vendors (paginated)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved successfully",
                vendorCacheService.listActiveVendors(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get vendor", description = "Get vendor details by ID")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> getVendor(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Vendor retrieved successfully",
                vendorCacheService.getVendorById(id)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Search vendors", description = "Search vendors by name (paginated)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchVendors(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved",
                vendorCacheService.searchVendors(query, page, size)));
    }

    // ── Vendor profile management (VENDOR role only) ────────────────────

    /**
     * Get the current vendor's own profile.
     * Returns 404 if the logged-in VENDOR user hasn't created a restaurant yet.
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get my vendor profile", description = "Get the logged-in vendor's restaurant profile")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> getMyVendorProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        var vendor = vendorRepository.findByUserId(userId).orElse(null);
        if (vendor == null) {
            return ResponseEntity.ok(ApiResponse.success("No vendor profile yet", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Vendor profile retrieved", vendorCacheService.toDTO(vendor)));
    }

    /**
     * Create a new restaurant profile for the logged-in vendor user.
     * A vendor user can only have one restaurant.
     */
    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Create vendor profile", description = "Create a new restaurant for the logged-in vendor")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> createVendorProfile(
            @Valid @RequestBody VendorCreateDTO dto,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());

        // Check if vendor already has a restaurant
        if (vendorRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("You already have a restaurant. Use PUT to update it.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Vendor vendor = Vendor.builder()
                .user(user)
                .name(dto.getName())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .lat(dto.getLat())
                .lng(dto.getLng())
                .openHours(dto.getOpenHours())
                .deliveryRadiusKm(dto.getDeliveryRadiusKm())
                .active(true)
                .build();

        vendor = vendorRepository.save(vendor);
        log.info("Vendor profile created: {} for user {}", vendor.getId(), userId);
        vendorCacheService.evictVendorListCaches();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Restaurant created successfully", vendorCacheService.toDTO(vendor)));
    }

    /**
     * Update the logged-in vendor's restaurant profile.
     */
    @PutMapping("/my")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update my vendor profile", description = "Update the logged-in vendor's restaurant details")
    public ResponseEntity<ApiResponse<VendorResponseDTO>> updateMyVendorProfile(
            @Valid @RequestBody VendorUpdateDTO dto,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No vendor profile found. Create one first."));

        if (dto.getName() != null) vendor.setName(dto.getName());
        if (dto.getDescription() != null) vendor.setDescription(dto.getDescription());
        if (dto.getAddress() != null) vendor.setAddress(dto.getAddress());
        if (dto.getLat() != null) vendor.setLat(dto.getLat());
        if (dto.getLng() != null) vendor.setLng(dto.getLng());
        if (dto.getOpenHours() != null) vendor.setOpenHours(dto.getOpenHours());
        if (dto.getActive() != null) vendor.setActive(dto.getActive());
        if (dto.getDeliveryRadiusKm() != null) vendor.setDeliveryRadiusKm(dto.getDeliveryRadiusKm());

        vendor = vendorRepository.save(vendor);
        log.info("Vendor profile updated: {} for user {}", vendor.getId(), userId);
        vendorCacheService.evictAllVendorCaches();

        return ResponseEntity.ok(ApiResponse.success("Restaurant updated successfully", vendorCacheService.toDTO(vendor)));
    }

    // ── Live GPS sharing for mobile vendors ──────────────────────────────

    /**
     * Update the vendor's live GPS location (for mobile food trucks / stalls).
     * Lightweight endpoint optimised for frequent calls from a mobile device.
     */
    @PutMapping("/my/location")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Update live GPS location", description = "Share the vendor's current GPS coordinates (for mobile vendors)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateVendorLocation(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        UUID userId = UUID.fromString(authentication.getName());
        Vendor vendor = vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No vendor profile found"));

        Number latNum = (Number) body.get("lat");
        Number lngNum = (Number) body.get("lng");
        if (latNum == null || lngNum == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("lat and lng are required"));
        }

        vendor.setLat(new java.math.BigDecimal(latNum.toString()));
        vendor.setLng(new java.math.BigDecimal(lngNum.toString()));
        vendor = vendorRepository.save(vendor);
        vendorCacheService.evictAllVendorCaches();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("vendorId", vendor.getId());
        result.put("lat", vendor.getLat());
        result.put("lng", vendor.getLng());
        result.put("updatedAt", vendor.getUpdatedAt() != null ? vendor.getUpdatedAt().toString() : null);

        log.info("Vendor {} updated live location to ({}, {})", vendor.getId(), vendor.getLat(), vendor.getLng());
        return ResponseEntity.ok(ApiResponse.success("Location updated", result));
    }

    // ── Delivery radius check ────────────────────────────────────────────

    /**
     * Check if a delivery address (lat/lng) is within a vendor's delivery radius.
     * Returns {inRange: boolean, distanceKm: double, radiusKm: double}
     */
    @GetMapping("/{id}/delivery-check")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Check delivery range", description = "Check if lat/lng is within vendor's delivery radius")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkDeliveryRange(
            @PathVariable UUID id,
            @RequestParam double lat,
            @RequestParam double lng
    ) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));

        if (vendor.getLat() == null || vendor.getLng() == null) {
            return ResponseEntity.ok(ApiResponse.success("Vendor location not set", Map.of(
                    "inRange", true, "distanceKm", 0, "radiusKm", 0, "vendorLocationSet", false
            )));
        }

        double distanceKm = haversineDistance(
                vendor.getLat().doubleValue(), vendor.getLng().doubleValue(), lat, lng);
        double radiusKm = vendor.getDeliveryRadiusKm() != null ? vendor.getDeliveryRadiusKm().doubleValue() : 10.0;
        boolean inRange = distanceKm <= radiusKm;

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("inRange", inRange);
        result.put("distanceKm", Math.round(distanceKm * 100.0) / 100.0);
        result.put("radiusKm", radiusKm);
        result.put("vendorLocationSet", true);

        return ResponseEntity.ok(ApiResponse.success(
                inRange ? "Within delivery range" : "Outside delivery range", result));
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
