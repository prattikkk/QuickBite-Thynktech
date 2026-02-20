package com.quickbite.common.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for checking and storing idempotency keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyRepository repository;

    private static final int EXPIRY_HOURS = 24;

    /**
     * Check whether this idempotency key has been used before.
     * @return the existing record if found (with a stored response), or empty if new.
     */
    @Transactional(readOnly = true)
    public Optional<IdempotencyKey> findExisting(String key, UUID userId, String endpoint) {
        return repository.findValidKey(key, userId, endpoint, OffsetDateTime.now());
    }

    /**
     * Store a new idempotency key with the response so future duplicates can return it.
     */
    @Transactional
    public IdempotencyKey store(String key, UUID userId, String endpoint,
                                String requestHash, int responseStatus, String responseBody) {
        IdempotencyKey record = IdempotencyKey.builder()
                .key(key)
                .userId(userId)
                .endpoint(endpoint)
                .requestHash(requestHash)
                .responseStatus(responseStatus)
                .responseBody(responseBody)
                .used(true)
                .expiresAt(OffsetDateTime.now().plusHours(EXPIRY_HOURS))
                .build();
        return repository.save(record);
    }

    /**
     * Purge expired keys — runs every hour.
     */
    @Scheduled(fixedRate = 3600_000)
    @Transactional
    public void purgeExpired() {
        int deleted = repository.deleteExpiredKeys(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
