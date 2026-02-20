package com.quickbite.orders.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Auditable timeline entry for every order state change.
 * Captures actor, role, event type, old/new status, and arbitrary metadata.
 */
@Entity
@Table(name = "event_timeline", indexes = {
    @Index(name = "idx_event_timeline_order", columnList = "order_id, created_at"),
    @Index(name = "idx_event_timeline_actor", columnList = "actor_id"),
    @Index(name = "idx_event_timeline_type", columnList = "event_type")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EventTimeline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "uuid")
    private UUID orderId;

    @Column(name = "actor_id", columnDefinition = "uuid")
    private UUID actorId;

    @Column(name = "actor_role", length = 50)
    private String actorRole;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "old_status", length = 50)
    private String oldStatus;

    @Column(name = "new_status", length = 50)
    private String newStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;
}
