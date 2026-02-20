package com.quickbite.audit.entity;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * AuditLog entity for system-wide audit trail.
 * Tracks all important actions for security and compliance.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_auditlog_user", columnList = "user_id"),
    @Index(name = "idx_auditlog_entity", columnList = "entity, entity_id"),
    @Index(name = "idx_auditlog_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 100)
    private String entity;

    @Column(name = "entity_id", columnDefinition = "uuid")
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private Map<String, Object> newValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> meta;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Common audit actions
     */
    public static class Action {
        public static final String CREATE = "CREATE";
        public static final String UPDATE = "UPDATE";
        public static final String DELETE = "DELETE";
        public static final String LOGIN = "LOGIN";
        public static final String LOGOUT = "LOGOUT";
        public static final String LOGIN_FAILED = "LOGIN_FAILED";
        public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
        public static final String ORDER_PLACED = "ORDER_PLACED";
        public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
        public static final String PAYMENT_PROCESSED = "PAYMENT_PROCESSED";
    }
}
