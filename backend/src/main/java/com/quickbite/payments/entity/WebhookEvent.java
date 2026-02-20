package com.quickbite.payments.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity for storing processed webhook events (idempotency).
 */
@Entity
@Table(name = "webhook_events", indexes = {
    @Index(name = "idx_webhook_provider_event_id", columnList = "provider_event_id", unique = true),
    @Index(name = "idx_webhook_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "provider_event_id", unique = true, nullable = false, length = 255)
    private String providerEventId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private Boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at", columnDefinition = "timestamptz")
    private OffsetDateTime processedAt;
}
