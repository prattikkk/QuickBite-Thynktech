package com.quickbite.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payments.entity.WebhookDlq;
import com.quickbite.payments.entity.WebhookEvent;
import com.quickbite.payments.repository.WebhookDlqRepository;
import com.quickbite.payments.repository.WebhookEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookProcessorService (retry + DLQ).
 */
@ExtendWith(MockitoExtension.class)
class WebhookProcessorServiceTest {

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookDlqRepository webhookDlqRepository;

    @Mock
    private WebhookEventProcessor webhookEventProcessor;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private WebhookProcessorService service;

    @BeforeEach
    void setUp() {
        service = new WebhookProcessorService(
                webhookEventRepository, webhookDlqRepository,
                webhookEventProcessor, objectMapper,
                new SimpleMeterRegistry());
    }

    private WebhookEvent createEvent(int attempts, int maxAttempts) {
        return WebhookEvent.builder()
                .id(UUID.randomUUID())
                .providerEventId("evt_" + UUID.randomUUID())
                .eventType("payment.captured")
                .payload("{\"id\":\"evt_1\",\"type\":\"payment.captured\",\"data\":{\"payment_id\":\"pi_123\"}}")
                .processed(false)
                .attempts(attempts)
                .maxAttempts(maxAttempts)
                .build();
    }

    // ========== processRetries ==========

    @Test
    void processRetries_noPending_doesNothing() {
        when(webhookEventRepository.findPendingRetries(any())).thenReturn(List.of());

        service.processRetries();

        verifyNoInteractions(webhookEventProcessor);
    }

    @Test
    void processRetries_processesPendingEvents() throws Exception {
        WebhookEvent event = createEvent(1, 5);
        when(webhookEventRepository.findPendingRetries(any())).thenReturn(List.of(event));
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), eq("payment.captured"))).thenReturn(true);

        service.processRetries();

        verify(webhookEventProcessor).processWebhookEvent(any(JsonNode.class), eq("payment.captured"));
        assertThat(event.getProcessed()).isTrue();
    }

    // ========== processEvent - Success ==========

    @Test
    void processEvent_success_marksProcessed() throws Exception {
        WebhookEvent event = createEvent(0, 5);
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), anyString())).thenReturn(true);

        service.processEvent(event);

        assertThat(event.getProcessed()).isTrue();
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getAttempts()).isEqualTo(1);
        verify(webhookEventRepository).save(event);
        verifyNoInteractions(webhookDlqRepository);
    }

    // ========== processEvent - Failure with Retry ==========

    @Test
    void processEvent_failure_schedulesRetry() throws Exception {
        WebhookEvent event = createEvent(0, 5);
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), anyString())).thenReturn(false);

        service.processEvent(event);

        assertThat(event.getProcessed()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("Processing returned false");
        assertThat(event.getNextRetryAt()).isAfter(OffsetDateTime.now().plusSeconds(20)); // ~30s backoff
        verify(webhookEventRepository).save(event);
        verifyNoInteractions(webhookDlqRepository);
    }

    @Test
    void processEvent_exceptionFailure_schedulesRetry() throws Exception {
        WebhookEvent event = createEvent(1, 5);
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), anyString()))
                .thenThrow(new RuntimeException("DB connection lost"));

        service.processEvent(event);

        assertThat(event.getProcessed()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(2);
        assertThat(event.getLastError()).isEqualTo("DB connection lost");
        assertThat(event.getNextRetryAt()).isNotNull();
        verify(webhookEventRepository).save(event);
    }

    // ========== processEvent - Exhausted → DLQ ==========

    @Test
    void processEvent_exhaustedRetries_movesToDlq() throws Exception {
        WebhookEvent event = createEvent(4, 5); // 4 attempts done, max 5 → this attempt will be #5
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), anyString())).thenReturn(false);

        service.processEvent(event);

        // Event marked as processed (moved to DLQ)
        assertThat(event.getProcessed()).isTrue();
        assertThat(event.getAttempts()).isEqualTo(5);

        // DLQ entry created
        ArgumentCaptor<WebhookDlq> dlqCaptor = ArgumentCaptor.forClass(WebhookDlq.class);
        verify(webhookDlqRepository).save(dlqCaptor.capture());

        WebhookDlq dlq = dlqCaptor.getValue();
        assertThat(dlq.getProviderEventId()).isEqualTo(event.getProviderEventId());
        assertThat(dlq.getEventType()).isEqualTo("payment.captured");
        assertThat(dlq.getOriginalEvent()).isEqualTo(event);
        assertThat(dlq.getAttempts()).isEqualTo(5);
    }

    // ========== Exponential Backoff ==========

    @Test
    void processEvent_backoffDoublesWithAttempts() throws Exception {
        // Attempt 1 → 30s, Attempt 2 → 60s, Attempt 3 → 120s
        WebhookEvent event1 = createEvent(0, 5);
        when(webhookEventProcessor.processWebhookEvent(any(JsonNode.class), anyString())).thenReturn(false);

        service.processEvent(event1);
        OffsetDateTime retry1 = event1.getNextRetryAt();

        WebhookEvent event2 = createEvent(1, 5);
        service.processEvent(event2);
        OffsetDateTime retry2 = event2.getNextRetryAt();

        WebhookEvent event3 = createEvent(2, 5);
        service.processEvent(event3);
        OffsetDateTime retry3 = event3.getNextRetryAt();

        // Each subsequent retry should be further in the future than the previous
        // (all measured from approximately the same "now")
        assertThat(retry2).isAfter(retry1);
        assertThat(retry3).isAfter(retry2);
    }
}
