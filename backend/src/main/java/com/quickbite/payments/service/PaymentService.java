package com.quickbite.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.payments.config.PaymentProperties;
import com.quickbite.payments.dto.PaymentIntentRequest;
import com.quickbite.payments.dto.PaymentIntentResponse;
import com.quickbite.payments.entity.Payment;
import com.quickbite.payments.entity.PaymentMethod;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.payments.entity.WebhookEvent;
import com.quickbite.payments.repository.PaymentRepository;
import com.quickbite.payments.repository.WebhookEventRepository;
import com.quickbite.payments.security.WebhookSecurityUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCaptureParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payment service implementation for Day 5.
 * Supports payment provider integration with webhook handling, idempotency, and order integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final PaymentProperties paymentProperties;
    private final ObjectMapper objectMapper;
    private final WebhookEventProcessor webhookEventProcessor;

    /**
     * Create payment intent for an order.
     * Integrates with payment provider (stubbed for now, ready for Razorpay/Stripe SDK).
     *
     * @param request payment intent request
     * @return PaymentIntentResponse with client secret
     */
    @Transactional
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) {
        log.info("Creating payment intent for order {} with currency {}", request.getOrderId(), request.getCurrency());

        // 1. Validate order exists
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        // 2. Check if payment already exists for this order (idempotency)
        if (order.getPayment() != null) {
            Payment existingPayment = paymentRepository.findById(order.getPayment().getId())
                    .orElse(null);
            if (existingPayment != null && existingPayment.getStatus() != PaymentStatus.FAILED) {
                log.info("Payment already exists for order {}: {}", order.getId(), existingPayment.getId());
                return mapToResponse(existingPayment);
            }
        }

        Long amountCents = order.getTotalCents();
        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency() : "INR";

        // 3. Decide payment path: CARD (Stripe) vs UPI/COD (offline)
        boolean isCOD = order.getPaymentMethod() == PaymentMethod.CASH_ON_DELIVERY;
        boolean isUPI = order.getPaymentMethod() == PaymentMethod.UPI;
        boolean isCard = order.getPaymentMethod() == PaymentMethod.CARD;
        String providerPaymentId;
        String clientSecret = null;

        if (isCard && isStripeConfigured()) {
            // -- Real Stripe PaymentIntent --
            try {
                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountCents)
                        .setCurrency(currency.toLowerCase())
                        .addPaymentMethodType("card")
                        .build();
                PaymentIntent stripePI = PaymentIntent.create(params);
                providerPaymentId = stripePI.getId();
                clientSecret = stripePI.getClientSecret();
                log.info("Stripe PaymentIntent created: {}", providerPaymentId);
            } catch (StripeException e) {
                log.error("Stripe API error creating payment intent", e);
                throw new RuntimeException("Payment gateway error: " + e.getUserMessage(), e);
            }
        } else if (isCOD) {
            providerPaymentId = "cod_" + UUID.randomUUID();
            log.info("COD payment — no Stripe intent needed");
        } else if (isUPI) {
            providerPaymentId = "upi_" + UUID.randomUUID();
            log.info("UPI payment — handled offline, no Stripe intent needed");
        } else {
            // Stripe not configured — stub fallback
            providerPaymentId = "stub_pi_" + UUID.randomUUID();
            clientSecret = "stub_secret_" + providerPaymentId;
            log.warn("Stripe not configured — using stub payment ID");
        }

        // 4. Create payment record in DB
        Payment payment = Payment.builder()
                .order(order)
                .providerPaymentId(providerPaymentId)
                .clientSecret(clientSecret)
                .provider(isStripeConfigured() ? "stripe" : "stub")
                .status(PaymentStatus.PENDING)
                .amountCents(amountCents)
                .currency(currency)
                .build();

        payment = paymentRepository.save(payment);
        log.info("Payment intent created: {} with provider ID: {}", payment.getId(), providerPaymentId);

        // 5. Link payment to order
        order.setPayment(payment);
        order.setPaymentStatus(PaymentStatus.PENDING);
        orderRepository.save(order);

        return mapToResponse(payment);
    }

    /**
     * Capture payment (called on order delivery).
     *
     * @param providerPaymentId provider payment ID
     * @param amountCents optional amount to capture (null = full amount)
     */
    @Transactional
    public void capturePayment(String providerPaymentId, Long amountCents) {
        log.info("Capturing payment: {}", providerPaymentId);

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + providerPaymentId));

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            log.warn("Payment already captured: {}", providerPaymentId);
            return;
        }

        // Call provider API to capture payment (stubbed)
        captureProviderPayment(providerPaymentId, amountCents);

        // Update payment status
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setPaidAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        // Update order payment status
        updateOrderPaymentStatus(payment.getId(), PaymentStatus.CAPTURED);

        log.info("Payment captured successfully: {}", providerPaymentId);
    }

    /**
     * Refund payment (called on order cancellation).
     *
     * @param providerPaymentId provider payment ID
     * @param amountCents optional amount to refund (null = full amount)
     * @param reason refund reason
     */
    @Transactional
    public void refundPayment(String providerPaymentId, Long amountCents, String reason) {
        log.info("Refunding payment: {} - Reason: {}", providerPaymentId, reason);

        Payment payment = paymentRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + providerPaymentId));

        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            log.warn("Payment already refunded: {}", providerPaymentId);
            return;
        }

        // Call provider API to refund payment (stubbed)
        refundProviderPayment(providerPaymentId, amountCents, reason);

        // Update payment status
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        // Update order payment status
        updateOrderPaymentStatus(payment.getId(), PaymentStatus.REFUNDED);

        log.info("Payment refunded successfully: {}", providerPaymentId);
    }

    /**
     * Handle webhook from payment provider.
     * Verifies signature, ensures idempotency, stores for async processing.
     *
     * @param rawBody         webhook payload
     * @param signatureHeader signature header
     * @return true if accepted (will be processed asynchronously)
     */
    @Transactional
    public boolean handleWebhook(String rawBody, String signatureHeader) {
        try {
            log.info("Processing webhook event");

            // 1. Verify signature
            if (!verifyWebhookSignature(rawBody, signatureHeader)) {
                log.warn("Webhook signature verification failed");
                return false;
            }

            // 2. Parse event
            JsonNode root = objectMapper.readTree(rawBody);
            String providerEventId = extractEventId(root);
            String eventType = extractEventType(root);

            // 3. Check idempotency - has this event been processed before?
            if (webhookEventRepository.existsByProviderEventId(providerEventId)) {
                log.info("Webhook event {} already received (idempotent)", providerEventId);
                return true;
            }

            // 4. Store webhook event for async processing (not processed yet)
            WebhookEvent webhookEvent = WebhookEvent.builder()
                    .providerEventId(providerEventId)
                    .eventType(eventType)
                    .payload(rawBody)
                    .processed(false)
                    .attempts(0)
                    .maxAttempts(5)
                    .build();
            webhookEventRepository.save(webhookEvent);

            // 5. Attempt immediate processing (best-effort synchronous first try)
            try {
                boolean processed = webhookEventProcessor.processWebhookEvent(root, eventType);
                if (processed) {
                    webhookEvent.setProcessed(true);
                    webhookEvent.setProcessedAt(OffsetDateTime.now());
                    webhookEvent.setAttempts(1);
                    webhookEventRepository.save(webhookEvent);
                    log.info("Webhook event {} processed immediately", providerEventId);
                } else {
                    // Will be picked up by WebhookProcessorService retry loop
                    webhookEvent.setAttempts(1);
                    webhookEvent.setLastError("Initial processing returned false");
                    webhookEvent.setNextRetryAt(OffsetDateTime.now().plusSeconds(30));
                    webhookEventRepository.save(webhookEvent);
                }
            } catch (Exception e) {
                webhookEvent.setAttempts(1);
                webhookEvent.setLastError(e.getMessage());
                webhookEvent.setNextRetryAt(OffsetDateTime.now().plusSeconds(30));
                webhookEventRepository.save(webhookEvent);
                log.warn("Webhook event {} initial processing failed, will retry: {}", providerEventId, e.getMessage());
            }

            return true; // Always ACK receipt to the webhook provider

        } catch (Exception e) {
            log.error("Error receiving webhook", e);
            return false;
        }
    }

    /**
     * Verify webhook signature based on provider.
     */
    private boolean verifyWebhookSignature(String payload, String signature) {
        String webhookSecret = paymentProperties.getWebhook().getSecret();
        String provider = paymentProperties.getProvider();

        if ("razorpay".equalsIgnoreCase(provider)) {
            return WebhookSecurityUtil.verifyRazorpaySignature(payload, signature, webhookSecret);
        } else if ("stripe".equalsIgnoreCase(provider)) {
            return WebhookSecurityUtil.verifyStripeSignature(payload, signature, webhookSecret);
        } else {
            // Generic HMAC-SHA256 verification
            return WebhookSecurityUtil.verifySignatureHmacSha256(payload, signature, webhookSecret);
        }
    }

    private String extractEventId(JsonNode root) {
        // Try common field names
        if (root.has("id")) return root.path("id").asText();
        if (root.has("event_id")) return root.path("event_id").asText();
        return UUID.randomUUID().toString(); // Fallback
    }

    private String extractEventType(JsonNode root) {
        if (root.has("event")) return root.path("event").asText();
        if (root.has("type")) return root.path("type").asText();
        return "unknown";
    }

    /**
     * Helper: Create provider payment intent (stub - replace with SDK call).
     * @deprecated Stripe calls are now inlined in createPaymentIntent().
     */
    @SuppressWarnings("unused")
    private String createProviderPaymentIntent(Long amountCents, String currency) {
        return "stub_pi_" + UUID.randomUUID().toString();
    }

    /**
     * Check if Stripe is configured (API key looks valid).
     */
    private boolean isStripeConfigured() {
        String apiKey = paymentProperties.getApiKey();
        return apiKey != null && (apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_"));
    }

    /**
     * Helper: Capture provider payment via Stripe.
     */
    private void captureProviderPayment(String providerPaymentId, Long amountCents) {
        if (!isStripeConfigured() || !providerPaymentId.startsWith("pi_")) {
            log.info("Skipping Stripe capture for non-Stripe payment: {}", providerPaymentId);
            return;
        }
        try {
            PaymentIntent pi = PaymentIntent.retrieve(providerPaymentId);
            if ("requires_capture".equals(pi.getStatus())) {
                PaymentIntentCaptureParams params = amountCents != null
                        ? PaymentIntentCaptureParams.builder().setAmountToCapture(amountCents).build()
                        : PaymentIntentCaptureParams.builder().build();
                pi.capture(params);
            }
            log.info("Stripe capture completed for: {}", providerPaymentId);
        } catch (StripeException e) {
            log.error("Stripe capture failed for {}: {}", providerPaymentId, e.getMessage(), e);
        }
    }

    /**
     * Helper: Refund provider payment via Stripe.
     */
    private void refundProviderPayment(String providerPaymentId, Long amountCents, String reason) {
        if (!isStripeConfigured() || !providerPaymentId.startsWith("pi_")) {
            log.info("Skipping Stripe refund for non-Stripe payment: {}", providerPaymentId);
            return;
        }
        try {
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(providerPaymentId);
            if (amountCents != null && amountCents > 0) {
                paramsBuilder.setAmount(amountCents);
            }
            Refund.create(paramsBuilder.build());
            log.info("Stripe refund completed for: {}", providerPaymentId);
        } catch (StripeException e) {
            log.error("Stripe refund failed for {}: {}", providerPaymentId, e.getMessage(), e);
        }
    }

    /**
     * Map Payment entity to response DTO.
     */
    private PaymentIntentResponse mapToResponse(Payment payment) {
        return PaymentIntentResponse.builder()
                .id(payment.getId())
                .providerPaymentId(payment.getProviderPaymentId())
                .clientSecret(payment.getClientSecret())
                .amountCents(payment.getAmountCents())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .build();
    }

    /**
     * Update order payment status when payment status changes.
     */
    private void updateOrderPaymentStatus(UUID paymentId, PaymentStatus status) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment != null && payment.getOrder() != null) {
            Order order = payment.getOrder();
            order.setPaymentStatus(status);
            orderRepository.save(order);
            log.info("Updated order {} payment status to {}", order.getId(), status);
        }
    }

    /**
     * Get payment by ID.
     */
    @Transactional(readOnly = true)
    public PaymentIntentResponse getPaymentById(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        return mapToResponse(payment);
    }

    // Keep old methods for backward compatibility (used by OrderService)
    
    /**
     * Create payment intent (old signature for backward compatibility).
     */
    @Transactional
    public Payment createPaymentIntent(UUID orderId, Long amountCents, String currency) {
        PaymentIntentRequest request = PaymentIntentRequest.builder()
                .orderId(orderId)
                .currency(currency)
                .build();
        
        PaymentIntentResponse response = createPaymentIntent(request);
        
        return paymentRepository.findById(response.getId())
                .orElseThrow(() -> new IllegalStateException("Payment not found after creation"));
    }

    /**
     * Capture payment (old signature for backward compatibility).
     */
    @Transactional
    public Payment capturePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        capturePayment(payment.getProviderPaymentId(), null);
        
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found after capture"));
    }

    /**
     * Authorize payment (old signature for backward compatibility).
     */
    @Transactional
    public Payment authorizePayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment = paymentRepository.save(payment);

        log.info("Payment authorized: {}", paymentId);
        return payment;
    }

    /**
     * Refund payment (old signature for backward compatibility).
     */
    @Transactional
    public Payment refundPayment(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
        
        refundPayment(payment.getProviderPaymentId(), null, "Order cancelled");
        
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("Payment not found after refund"));
    }
}
