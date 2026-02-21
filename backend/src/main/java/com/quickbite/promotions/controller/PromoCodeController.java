package com.quickbite.promotions.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.promotions.dto.PromoCodeDTO;
import com.quickbite.promotions.dto.PromoCreateRequest;
import com.quickbite.promotions.dto.PromoValidateResponse;
import com.quickbite.promotions.service.PromoCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for promo code management.
 * - Customer: validate promo code
 * - Admin: CRUD promo codes
 */
@Slf4j
@RestController
@RequestMapping("/api/promos")
@RequiredArgsConstructor
@Tag(name = "Promo Codes", description = "Promotion and coupon management")
@SecurityRequirement(name = "bearerAuth")
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    // ========== Customer endpoints ==========

    @GetMapping("/validate")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Validate promo code", description = "Validate a promo code and get discount preview")
    public ResponseEntity<ApiResponse<PromoValidateResponse>> validatePromo(
            @RequestParam String code,
            @RequestParam long subtotalCents
    ) {
        PromoValidateResponse result = promoCodeService.validatePromo(code, subtotalCents);
        return ResponseEntity.ok(ApiResponse.success("Validation complete", result));
    }

    // ========== Admin endpoints ==========

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create promo code", description = "Admin creates a new promo code")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> createPromo(@Valid @RequestBody PromoCreateRequest req) {
        PromoCodeDTO dto = promoCodeService.createPromo(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Promo created", dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update promo code", description = "Admin updates a promo code")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> updatePromo(
            @PathVariable UUID id,
            @Valid @RequestBody PromoCreateRequest req
    ) {
        PromoCodeDTO dto = promoCodeService.updatePromo(id, req);
        return ResponseEntity.ok(ApiResponse.success("Promo updated", dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all promos", description = "Admin lists all promo codes")
    public ResponseEntity<ApiResponse<List<PromoCodeDTO>>> listAll() {
        List<PromoCodeDTO> promos = promoCodeService.listAllPromos();
        return ResponseEntity.ok(ApiResponse.success("Promos retrieved", promos));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get promo code", description = "Admin gets promo code details")
    public ResponseEntity<ApiResponse<PromoCodeDTO>> getPromo(@PathVariable UUID id) {
        PromoCodeDTO dto = promoCodeService.getPromo(id);
        return ResponseEntity.ok(ApiResponse.success("Promo retrieved", dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete promo code", description = "Admin deletes a promo code")
    public ResponseEntity<ApiResponse<Void>> deletePromo(@PathVariable UUID id) {
        promoCodeService.deletePromo(id);
        return ResponseEntity.ok(ApiResponse.success("Promo deleted", null));
    }
}
