package com.quickbite.delivery.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.delivery.dto.DeliveryProofResponseDTO;
import com.quickbite.delivery.dto.OtpVerifyRequestDTO;
import com.quickbite.delivery.service.DeliveryProofService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST controller for proof-of-delivery operations.
 * Phase 3 — Photo proof, OTP generation & verification.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders/{orderId}/proof")
@RequiredArgsConstructor
@Tag(name = "Delivery Proof", description = "Proof-of-delivery endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ProofController {

    private final DeliveryProofService proofService;

    /**
     * Submit photo proof of delivery.
     * Accepts multipart/form-data with photo file + optional metadata.
     */
    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Submit photo proof", description = "Upload delivery photo as proof")
    public ResponseEntity<ApiResponse<DeliveryProofResponseDTO>> submitPhotoProof(
            @PathVariable UUID orderId,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "lat", required = false) BigDecimal lat,
            @RequestParam(value = "lng", required = false) BigDecimal lng,
            Authentication auth) {

        UUID driverId = extractUserId(auth);
        log.info("Driver {} submitting photo proof for order {}", driverId, orderId);

        DeliveryProofResponseDTO result = proofService.submitPhotoProof(
                orderId, driverId, photo, notes, lat, lng);

        return ResponseEntity.ok(ApiResponse.success("Photo proof submitted", result));
    }

    /**
     * Generate OTP for delivery confirmation.
     * Sends OTP to customer via notification.
     */
    @PostMapping("/otp/generate")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Generate delivery OTP", description = "Generate 6-digit OTP for customer confirmation")
    public ResponseEntity<ApiResponse<DeliveryProofResponseDTO>> generateOtp(
            @PathVariable UUID orderId,
            Authentication auth) {

        UUID driverId = extractUserId(auth);
        log.info("Driver {} generating OTP for order {}", driverId, orderId);

        DeliveryProofResponseDTO result = proofService.generateOtp(orderId, driverId);
        return ResponseEntity.ok(ApiResponse.success("OTP generated and sent to customer", result));
    }

    /**
     * Verify delivery OTP entered by driver.
     */
    @PostMapping("/otp/verify")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Verify delivery OTP", description = "Verify OTP code from customer")
    public ResponseEntity<ApiResponse<DeliveryProofResponseDTO>> verifyOtp(
            @PathVariable UUID orderId,
            @Valid @RequestBody OtpVerifyRequestDTO request,
            Authentication auth) {

        UUID driverId = extractUserId(auth);
        log.info("Driver {} verifying OTP for order {}", driverId, orderId);

        DeliveryProofResponseDTO result = proofService.verifyOtp(orderId, driverId, request.getCode());
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", result));
    }

    /**
     * Get proof for an order (accessible by customer, driver, vendor, admin).
     */
    @GetMapping
    @Operation(summary = "Get delivery proof", description = "Retrieve proof of delivery for an order")
    public ResponseEntity<ApiResponse<DeliveryProofResponseDTO>> getProof(
            @PathVariable UUID orderId) {

        return proofService.getProofByOrderId(orderId)
                .map(dto -> ResponseEntity.ok(ApiResponse.success("Proof found", dto)))
                .orElse(ResponseEntity.ok(ApiResponse.success("No proof submitted yet", (DeliveryProofResponseDTO) null)));
    }

    /**
     * Check if proof is still required for an order.
     */
    @GetMapping("/required")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Check proof requirement", description = "Check if proof is required before delivery")
    public ResponseEntity<ApiResponse<Boolean>> isProofRequired(@PathVariable UUID orderId) {
        boolean required = proofService.isProofRequired(orderId);
        return ResponseEntity.ok(ApiResponse.success(
                required ? "Proof required before marking delivered" : "No proof required", required));
    }

    private UUID extractUserId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
