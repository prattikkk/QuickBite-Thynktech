package com.quickbite.auth.entity;

import com.quickbite.users.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * TokenStore entity for managing JWT refresh tokens.
 * Enables token revocation and session management.
 */
@Entity
@Table(name = "token_store", indexes = {
    @Index(name = "idx_token_user", columnList = "user_id"),
    @Index(name = "idx_token_hash", columnList = "token_hash"),
    @Index(name = "idx_token_expires", columnList = "expires_at"),
    @Index(name = "idx_token_revoked", columnList = "revoked")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenStore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "token_type", length = 50)
    @Builder.Default
    private String tokenType = "REFRESH";

    @CreationTimestamp
    @Column(name = "issued_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", columnDefinition = "timestamptz", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_at", columnDefinition = "timestamptz")
    private OffsetDateTime revokedAt;

    @Column(name = "device_info", columnDefinition = "text")
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Check if token is valid (not revoked and not expired)
     */
    public boolean isValid() {
        return !revoked && expiresAt.isAfter(OffsetDateTime.now());
    }

    /**
     * Revoke this token
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = OffsetDateTime.now();
    }
}
