package com.quickbite.common.idempotency;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Stores idempotency keys so duplicate POST requests return the cached response.
 */
@Entity
@Table(name = "idempotency_keys",
       uniqueConstraints = @UniqueConstraint(
           name = "uq_idempotency_key_user",
           columnNames = {"idempotency_key", "user_id", "endpoint"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 255)
    private String key;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "text")
    private String responseBody;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", columnDefinition = "timestamptz")
    private OffsetDateTime expiresAt;
}
