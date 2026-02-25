package com.quickbite.auth.service;

import com.quickbite.auth.repository.PasswordResetTokenRepository;
import com.quickbite.auth.repository.TokenStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Scheduled service to prune expired and revoked tokens.
 * Prevents token_store and password_reset_tokens tables from growing unbounded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenStoreRepository tokenStoreRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Purge expired refresh tokens every 6 hours.
     * Deletes tokens whose expiresAt is in the past.
     */
    @Scheduled(fixedRate = 21_600_000) // 6 hours
    @Transactional
    public void purgeExpiredTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = tokenStoreRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Token cleanup: purged {} expired refresh tokens", deleted);
        }
    }

    /**
     * Purge expired password reset tokens every 2 hours.
     */
    @Scheduled(fixedRate = 7_200_000) // 2 hours
    @Transactional
    public void purgeExpiredPasswordResetTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = passwordResetTokenRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Token cleanup: purged {} expired password reset tokens", deleted);
        }
    }
}
