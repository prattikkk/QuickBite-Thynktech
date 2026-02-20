package com.quickbite.common.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Find a non-expired idempotency record by key + user + endpoint.
     */
    @Query("SELECT ik FROM IdempotencyKey ik " +
           "WHERE ik.key = :key AND ik.userId = :userId AND ik.endpoint = :endpoint " +
           "AND ik.expiresAt > :now")
    Optional<IdempotencyKey> findValidKey(String key, UUID userId, String endpoint, OffsetDateTime now);

    /**
     * Purge expired idempotency keys.
     */
    @Modifying
    @Query("DELETE FROM IdempotencyKey ik WHERE ik.expiresAt < :now")
    int deleteExpiredKeys(OffsetDateTime now);
}
