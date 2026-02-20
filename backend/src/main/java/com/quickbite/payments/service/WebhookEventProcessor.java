package com.quickbite.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.payments.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Processes webhook event payloads.
 * Extracted from PaymentService so it can be called by both the synchronous path
 * and the asynchronous retry processor (WebhookProcessorService).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookEventProcessor {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    /**
     * Process a parsed webhook event and update payment/order status accordingly.
     *
     * @param root      parsed JSON root of the webhook payload
     * @param eventType the event type string (e.g. "payment_intent.succeeded")
     * @return true if processed successfully, false if the event couldn't be handled
     */
    @Transactional
    public boolean processWebhookEvent(JsonNode root, String eventType) {
        try {
            JsonNode data = root.path("data");

            return switch (eventType) {
                case "payment.captured", "payment.success", "charge.succeeded", "payment_intent.succeeded" ->
                        handlePaymentCaptured(data);
                case "payment.failed", "charge.failed", "payment_intent.payment_failed" ->
                        handlePaymentFailed(data);
                case "payment.refunded", "charge.refunded" ->
                        handlePaymentRefunded(data);
                case "payment.authorized" ->
                        handlePaymentAuthorized(data);
                default -> {
                    log.info("Unknown webhook event type: {} — acknowledging", eventType);
                    yield true;
                }
            };
        } catch (Exception e) {
            log.error("Error processing webhook event type: {}", eventType, e);
            return false;
        }
    }

    private boolean handlePaymentCaptured(JsonNode data) {
        String pid = resolveProviderPaymentId(data);
        return paymentRepository.findByProviderPaymentId(pid)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.CAPTURED);
                    payment.setPaidAt(OffsetDateTime.now());
                    paymentRepository.save(payment);
                    updateOrderPaymentStatus(payment.getId(), PaymentStatus.CAPTURED);
                    return true;
                })
                .orElse(false);
    }

    private boolean handlePaymentFailed(JsonNode data) {
        String pid = resolveProviderPaymentId(data);
        String reasonRaw = data.path("error_description").asText("");
        if (reasonRaw.isBlank()) {
            reasonRaw = data.path("object").path("last_payment_error").path("message").asText("Payment failed");
        }
        final String reason = reasonRaw;
        return paymentRepository.findByProviderPaymentId(pid)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailedAt(OffsetDateTime.now());
                    payment.setFailureReason(reason);
                    paymentRepository.save(payment);
                    updateOrderPaymentStatus(payment.getId(), PaymentStatus.FAILED);
                    return true;
                })
                .orElse(false);
    }

    private boolean handlePaymentRefunded(JsonNode data) {
        String pid = resolveProviderPaymentId(data);
        return paymentRepository.findByProviderPaymentId(pid)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.REFUNDED);
                    paymentRepository.save(payment);
                    updateOrderPaymentStatus(payment.getId(), PaymentStatus.REFUNDED);
                    return true;
                })
                .orElse(false);
    }

    private boolean handlePaymentAuthorized(JsonNode data) {
        String pid = resolveProviderPaymentId(data);
        return paymentRepository.findByProviderPaymentId(pid)
                .map(payment -> {
                    payment.setStatus(PaymentStatus.AUTHORIZED);
                    paymentRepository.save(payment);
                    updateOrderPaymentStatus(payment.getId(), PaymentStatus.AUTHORIZED);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Extract provider payment ID from webhook data (supports Razorpay, Stripe, generic).
     */
    private String resolveProviderPaymentId(JsonNode data) {
        String id = data.path("payment_id").asText("");
        if (id.isBlank()) id = data.path("id").asText("");
        if (id.isBlank()) id = data.path("object").path("id").asText("");
        return id;
    }

    /**
     * Update order payment status when payment status changes.
     */
    private void updateOrderPaymentStatus(UUID paymentId, PaymentStatus status) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            orderRepository.findAll().stream()
                    .filter(o -> o.getPayment() != null && o.getPayment().getId().equals(paymentId))
                    .findFirst()
                    .ifPresent(order -> {
                        order.setPaymentStatus(status);
                        orderRepository.save(order);
                    });
        });
    }
}
