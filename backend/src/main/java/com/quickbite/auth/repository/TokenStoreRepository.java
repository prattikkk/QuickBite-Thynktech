package com.quickbite.auth.repository;

import com.quickbite.auth.entity.TokenStore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TokenStore entity operations.
 * Manages JWT refresh tokens and session control.
 */
@Repository
public interface TokenStoreRepository extends JpaRepository<TokenStore, UUID> {

    /**
     * Find a valid (non-revoked, non-expired) token by hash.
     *
     * @param tokenHash the hashed token
     * @return Optional containing the token if found and valid
     */
    @Query("SELECT t FROM TokenStore t WHERE t.tokenHash = :tokenHash " +
           "AND t.revoked = false AND t.expiresAt > :now")
    Optional<TokenStore> findValidTokenByHash(
        @Param("tokenHash") String tokenHash,
        @Param("now") OffsetDateTime now
    );

    /**
     * Find all valid tokens for a user.
     *
     * @param userId the user ID
     * @return List of valid tokens
     */
    @Query("SELECT t FROM TokenStore t WHERE t.user.id = :userId " +
           "AND t.revoked = false AND t.expiresAt > :now " +
           "ORDER BY t.issuedAt DESC")
    List<TokenStore> findValidTokensByUserId(
        @Param("userId") UUID userId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Find all tokens for a user (including revoked and expired).
     *
     * @param userId the user ID
     * @return List of tokens
     */
    List<TokenStore> findByUserId(UUID userId);

    /**
     * Revoke all tokens for a user (for logout all sessions).
     *
     * @param userId the user ID
     * @param revokedAt the revocation timestamp
     */
    @Modifying
    @Query("UPDATE TokenStore t SET t.revoked = true, t.revokedAt = :revokedAt " +
           "WHERE t.user.id = :userId AND t.revoked = false")
    void revokeAllUserTokens(
        @Param("userId") UUID userId,
        @Param("revokedAt") OffsetDateTime revokedAt
    );

    /**
     * Delete expired tokens (cleanup job).
     *
     * @param expiryDate delete tokens expired before this date
     */
    @Modifying
    @Query("DELETE FROM TokenStore t WHERE t.expiresAt < :expiryDate")
    void deleteExpiredTokens(@Param("expiryDate") OffsetDateTime expiryDate);

    /**
     * Count active tokens for a user.
     *
     * @param userId the user ID
     * @return count of active tokens
     */
    @Query("SELECT COUNT(t) FROM TokenStore t WHERE t.user.id = :userId " +
           "AND t.revoked = false AND t.expiresAt > :now")
    Long countActiveTokensByUserId(
        @Param("userId") UUID userId,
        @Param("now") OffsetDateTime now
    );

    /**
     * Find tokens by device info (for session management).
     *
     * @param userId the user ID
     * @param deviceInfo the device information
     * @return List of tokens
     */
    List<TokenStore> findByUserIdAndDeviceInfo(UUID userId, String deviceInfo);

    /**
     * Find token by hash.
     *
     * @param tokenHash the hashed token
     * @return Optional containing the token if found
     */
    Optional<TokenStore> findByTokenHash(String tokenHash);

    /**
     * Find token by hash and revoked status.
     *
     * @param tokenHash the hashed token
     * @param revoked the revoked status
     * @return Optional containing the token if found
     */
    Optional<TokenStore> findByTokenHashAndRevoked(String tokenHash, boolean revoked);

    /**
     * Delete tokens that expired before the given date.
     *
     * @param expiryDate the expiry date
     * @return number of deleted tokens
     */
    @Modifying
    int deleteByExpiresAtBefore(OffsetDateTime expiryDate);
}
