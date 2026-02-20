package com.quickbite.payments.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.payments.dto.*;
import com.quickbite.payments.service.PaymentService;
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

import java.util.UUID;

/**
 * REST controller for payment operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment integration endpoints")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create payment intent for an order.
     */
    @PostMapping("/intent")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Create payment intent", description = "Create a payment intent for an order")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request) {
        log.info("Creating payment intent for order: {}", request.getOrderId());
        
        PaymentIntentResponse response = paymentService.createPaymentIntent(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment intent created successfully", response));
    }

    /**
     * Get payment by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "Get payment", description = "Get payment details by ID")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> getPayment(@PathVariable UUID id) {
        log.info("Getting payment: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", paymentService.getPaymentById(id)));
    }

    /**
     * Capture payment (called on order delivery).
     */
    @PostMapping("/capture")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    @Operation(summary = "Capture payment", description = "Capture a previously authorized payment")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> capturePayment(
            @Valid @RequestBody CapturePaymentRequest request) {
        log.info("Capturing payment: {}", request.getProviderPaymentId());
        
        paymentService.capturePayment(request.getProviderPaymentId(), request.getAmountCents());
        
        return ResponseEntity.ok(ApiResponse.success("Payment captured successfully", null));
    }

    /**
     * Refund payment (called on order cancellation).
     */
    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Refund payment", description = "Refund a captured payment")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> refundPayment(
            @Valid @RequestBody RefundPaymentRequest request) {
        log.info("Refunding payment: {} - Reason: {}", request.getProviderPaymentId(), request.getReason());
        
        paymentService.refundPayment(request.getProviderPaymentId(), request.getAmountCents(), request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("Refund initiated successfully", null));
    }

    /**
     * Webhook endpoint - payment provider posts events here.
     * Verify signature and process idempotently.
     * 
     * Note: This endpoint is NOT protected by JWT auth as it's called by external providers.
     * Security is handled via HMAC signature verification.
     */
    @PostMapping("/webhook")
    @Operation(summary = "Payment webhook", description = "Receive webhook events from payment provider")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature,
            @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
            @RequestBody String rawBody) {
        
        log.info("Received webhook with signature header present: {}", 
                signature != null || razorpaySignature != null || stripeSignature != null);
        
        // Use whichever signature header is present
        String actualSignature = signature != null ? signature : 
                                (razorpaySignature != null ? razorpaySignature : stripeSignature);
        
        boolean processed = paymentService.handleWebhook(rawBody, actualSignature);
        
        if (processed) {
            log.info("Webhook processed successfully");
            return ResponseEntity.ok("ok");
        } else {
            log.warn("Webhook processing failed - invalid signature or duplicate event");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("invalid");
        }
    }
}
