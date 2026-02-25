package com.quickbite.auth.repository;

import com.quickbite.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByTokenHashAndUsedFalse(String tokenHash);

    long countByUserIdAndCreatedAtAfter(UUID userId, OffsetDateTime after);

    int deleteByExpiresAtBefore(OffsetDateTime before);
}
