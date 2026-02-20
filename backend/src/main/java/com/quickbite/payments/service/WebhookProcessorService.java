package com.quickbite.payments.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.payments.entity.WebhookDlq;
import com.quickbite.payments.entity.WebhookEvent;
import com.quickbite.payments.repository.WebhookDlqRepository;
import com.quickbite.payments.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Async webhook processor with retry/backoff and dead-letter queue.
 * <p>
 * On webhook receipt, PaymentService stores the event with {@code processed = false}.
 * This service picks up unprocessed events periodically and attempts to process them.
 * On failure, increments attempts and sets exponential back-off {@code nextRetryAt}.
 * After max_attempts, the event is moved to the dead-letter queue.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookProcessorService {

    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqRepository webhookDlqRepository;
    private final WebhookEventProcessor webhookEventProcessor;
    private final ObjectMapper objectMapper;

    /** Base backoff in seconds — doubles each retry (30, 60, 120, 240, 480…). */
    private static final int BASE_BACKOFF_SECONDS = 30;

    /**
     * Poll for unprocessed webhook events and process them.
     * Runs every 15 seconds.
     */
    @Scheduled(fixedDelay = 15_000, initialDelay = 5_000)
    public void processRetries() {
        List<WebhookEvent> pending = webhookEventRepository.findPendingRetries(OffsetDateTime.now());
        if (pending.isEmpty()) return;

        log.info("Processing {} pending webhook events", pending.size());
        for (WebhookEvent event : pending) {
            processEvent(event);
        }
    }

    /**
     * Process a single webhook event. Called by the scheduled poller.
     */
    @Transactional
    public void processEvent(WebhookEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            String eventType = event.getEventType();

            boolean success = webhookEventProcessor.processWebhookEvent(root, eventType);

            if (success) {
                event.setProcessed(true);
                event.setProcessedAt(OffsetDateTime.now());
                event.setAttempts(event.getAttempts() + 1);
                webhookEventRepository.save(event);
                log.info("Webhook event {} processed successfully on attempt {}", event.getProviderEventId(), event.getAttempts());
            } else {
                handleFailure(event, "Processing returned false");
            }
        } catch (Exception e) {
            handleFailure(event, e.getMessage());
        }
    }

    private void handleFailure(WebhookEvent event, String error) {
        int newAttempts = event.getAttempts() + 1;
        event.setAttempts(newAttempts);
        event.setLastError(error);

        if (newAttempts >= event.getMaxAttempts()) {
            // Move to dead-letter queue
            moveToDlq(event, error);
            event.setProcessed(true); // Mark as processed (moved to DLQ)
            event.setProcessingError("Moved to DLQ after " + newAttempts + " attempts: " + error);
            event.setProcessedAt(OffsetDateTime.now());
            webhookEventRepository.save(event);
            log.warn("Webhook event {} moved to DLQ after {} attempts", event.getProviderEventId(), newAttempts);
        } else {
            // Schedule retry with exponential backoff
            int backoffSeconds = BASE_BACKOFF_SECONDS * (1 << (newAttempts - 1));
            event.setNextRetryAt(OffsetDateTime.now().plusSeconds(backoffSeconds));
            webhookEventRepository.save(event);
            log.info("Webhook event {} retry #{} scheduled in {}s", event.getProviderEventId(), newAttempts, backoffSeconds);
        }
    }

    private void moveToDlq(WebhookEvent event, String error) {
        WebhookDlq dlqEntry = WebhookDlq.builder()
                .providerEventId(event.getProviderEventId())
                .eventType(event.getEventType())
                .payload(event.getPayload())
                .errorMessage(error)
                .attempts(event.getAttempts())
                .originalEvent(event)
                .build();
        webhookDlqRepository.save(dlqEntry);
    }
}
