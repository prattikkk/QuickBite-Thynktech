package com.quickbite.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.payments.entity.Payment;
import com.quickbite.payments.entity.PaymentStatus;
import com.quickbite.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookEventProcessor.
 */
@ExtendWith(MockitoExtension.class)
class WebhookEventProcessorTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private WebhookEventProcessor processor;

    private final ObjectMapper mapper = new ObjectMapper();
    private UUID paymentId;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        testPayment = Payment.builder()
                .id(paymentId)
                .providerPaymentId("pi_123")
                .status(PaymentStatus.PENDING)
                .amountCents(50000L)
                .currency("INR")
                .build();
    }

    // ========== payment.captured / payment_intent.succeeded ==========

    @Test
    void processPaymentCaptured_updatesStatusToCaptured() throws Exception {
        // Arrange
        String json = """
                {
                    "id": "evt_1",
                    "type": "payment.captured",
                    "data": { "payment_id": "pi_123" }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        // Act
        boolean result = processor.processWebhookEvent(root, "payment.captured");

        // Assert
        assertThat(result).isTrue();
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
        assertThat(testPayment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(testPayment);
    }

    @Test
    void processPaymentIntentSucceeded_updatesStatusToCaptured() throws Exception {
        String json = """
                {
                    "id": "evt_2",
                    "type": "payment_intent.succeeded",
                    "data": { "object": { "id": "pi_123" } }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        boolean result = processor.processWebhookEvent(root, "payment_intent.succeeded");

        assertThat(result).isTrue();
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
    }

    // ========== payment.failed ==========

    @Test
    void processPaymentFailed_updatesStatusToFailed() throws Exception {
        String json = """
                {
                    "id": "evt_3",
                    "type": "payment.failed",
                    "data": { "payment_id": "pi_123", "error_description": "Card declined" }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        boolean result = processor.processWebhookEvent(root, "payment.failed");

        assertThat(result).isTrue();
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(testPayment.getFailureReason()).isEqualTo("Card declined");
        assertThat(testPayment.getFailedAt()).isNotNull();
    }

    // ========== payment.refunded ==========

    @Test
    void processPaymentRefunded_updatesStatusToRefunded() throws Exception {
        String json = """
                {
                    "id": "evt_4",
                    "type": "payment.refunded",
                    "data": { "payment_id": "pi_123" }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        boolean result = processor.processWebhookEvent(root, "payment.refunded");

        assertThat(result).isTrue();
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    // ========== payment.authorized ==========

    @Test
    void processPaymentAuthorized_updatesStatusToAuthorized() throws Exception {
        String json = """
                {
                    "id": "evt_5",
                    "type": "payment.authorized",
                    "data": { "payment_id": "pi_123" }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        boolean result = processor.processWebhookEvent(root, "payment.authorized");

        assertThat(result).isTrue();
        assertThat(testPayment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    }

    // ========== Unknown event type ==========

    @Test
    void processUnknownEventType_returnsTrue() throws Exception {
        String json = """
                {
                    "id": "evt_6",
                    "type": "some.unknown.event",
                    "data": {}
                }
                """;
        JsonNode root = mapper.readTree(json);

        boolean result = processor.processWebhookEvent(root, "some.unknown.event");

        assertThat(result).isTrue();
        verifyNoInteractions(paymentRepository);
    }

    // ========== Payment not found ==========

    @Test
    void processPaymentCaptured_paymentNotFound_returnsFalse() throws Exception {
        String json = """
                {
                    "id": "evt_7",
                    "type": "payment.captured",
                    "data": { "payment_id": "pi_nonexistent" }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_nonexistent")).thenReturn(Optional.empty());

        boolean result = processor.processWebhookEvent(root, "payment.captured");

        assertThat(result).isFalse();
        verify(paymentRepository, never()).save(any());
    }

    // ========== Provider ID resolution ==========

    @Test
    void processWebhook_resolvesStripeObjectId() throws Exception {
        // Stripe puts ID in data.object.id
        String json = """
                {
                    "id": "evt_8",
                    "type": "charge.succeeded",
                    "data": {
                        "object": { "id": "pi_123" }
                    }
                }
                """;
        JsonNode root = mapper.readTree(json);

        when(paymentRepository.findByProviderPaymentId("pi_123")).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any())).thenReturn(testPayment);

        boolean result = processor.processWebhookEvent(root, "charge.succeeded");

        assertThat(result).isTrue();
        verify(paymentRepository).findByProviderPaymentId("pi_123");
    }
}
