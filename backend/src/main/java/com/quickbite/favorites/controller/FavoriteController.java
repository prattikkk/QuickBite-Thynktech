package com.quickbite.favorites.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.favorites.dto.FavoriteDTO;
import com.quickbite.favorites.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for customer favorite vendors.
 */
@Slf4j
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Customer favorite vendor endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{vendorId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add favorite", description = "Add a vendor to favorites")
    public ResponseEntity<ApiResponse<FavoriteDTO>> addFavorite(
            @PathVariable UUID vendorId,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        FavoriteDTO dto = favoriteService.addFavorite(userId, vendorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Added to favorites", dto));
    }

    @DeleteMapping("/{vendorId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Remove favorite", description = "Remove a vendor from favorites")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable UUID vendorId,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        favoriteService.removeFavorite(userId, vendorId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", null));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "List favorites", description = "Get all favorite vendors")
    public ResponseEntity<ApiResponse<List<FavoriteDTO>>> listFavorites(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        List<FavoriteDTO> favorites = favoriteService.getFavorites(userId);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved", favorites));
    }

    @GetMapping("/{vendorId}/check")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Check favorite", description = "Check if vendor is a favorite")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkFavorite(
            @PathVariable UUID vendorId,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        boolean isFav = favoriteService.isFavorite(userId, vendorId);
        return ResponseEntity.ok(ApiResponse.success("Checked", Map.of("isFavorite", isFav)));
    }

    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
