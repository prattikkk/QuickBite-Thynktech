package com.quickbite.auth.service;

import com.quickbite.auth.dto.ForgotPasswordRequest;
import com.quickbite.auth.dto.ResetPasswordRequest;
import com.quickbite.auth.dto.VerifyEmailRequest;
import com.quickbite.auth.entity.PasswordResetToken;
import com.quickbite.auth.exception.AuthException;
import com.quickbite.auth.repository.PasswordResetTokenRepository;
import com.quickbite.email.service.EmailDispatchService;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Handles password-reset and email-verification flows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailDispatchService emailDispatchService;

    private static final int TOKEN_EXPIRY_HOURS = 1;
    private static final int MAX_RESET_REQUESTS_PER_HOUR = 3;

    /* ---------- Forgot / Reset Password ---------- */

    /**
     * Request a password reset. Generates a random token and stores its hash.
     * Returns the raw token (to be emailed to the user in a real system).
     */
    @Transactional
    public String requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return null;
        }

        // Rate limit: max N resets per hour per user
        long recentCount = tokenRepository.countByUserIdAndCreatedAtAfter(
                user.getId(), OffsetDateTime.now().minusHours(1));
        if (recentCount >= MAX_RESET_REQUESTS_PER_HOUR) {
            log.warn("Rate limit hit for password reset: userId={}", user.getId());
            return null; // silent — no info leak
        }

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusHours(TOKEN_EXPIRY_HOURS))
                .build();
        tokenRepository.save(prt);

        log.info("Password reset token created for user: {}", user.getEmail());

        // Dispatch password-reset email (fire-and-forget)
        try {
            emailDispatchService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getName() != null ? user.getName() : "User",
                    rawToken);
        } catch (Exception e) {
            log.warn("Failed to dispatch password-reset email for {}: {}", user.getEmail(), e.getMessage());
        }

        return rawToken;
    }

    /**
     * Reset the password using the token received via email/link.
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = sha256(request.getToken());

        PasswordResetToken prt = tokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        if (prt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException("Reset token has expired");
        }

        User user = prt.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    /* ---------- Email Verification ---------- */

    /**
     * Generate an email-verification token for a user.
     * Re-uses the same password_reset_tokens table for simplicity.
     * The token type is identified by context (forgot-password vs. verify-email endpoints).
     */
    @Transactional
    public String createEmailVerificationToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        String rawToken = generateSecureToken();
        String tokenHash = sha256(rawToken);

        PasswordResetToken prt = PasswordResetToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusHours(24)) // 24-hour window
                .build();
        tokenRepository.save(prt);
        return rawToken;
    }

    /**
     * Verify the user's email address.
     */
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        String tokenHash = sha256(request.getToken());

        PasswordResetToken prt = tokenRepository.findByTokenHashAndUsedFalse(tokenHash)
                .orElseThrow(() -> new AuthException("Invalid or expired verification token"));

        if (prt.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AuthException("Verification token has expired");
        }

        User user = prt.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);

        log.info("Email verified for user: {}", user.getEmail());
    }

    /**
     * Resend email verification link. Creates a new token and dispatches the email.
     * Silent on unknown emails to prevent enumeration.
     */
    @Transactional
    public void resendEmailVerification(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.debug("Resend verification requested for unknown email: {}", email);
            return; // silent — no enumeration
        }
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            log.debug("Resend verification requested for already-verified email: {}", email);
            return;
        }
        String rawToken = createEmailVerificationToken(user.getId());
        emailDispatchService.sendEmailVerification(
                user.getEmail(),
                user.getName() != null ? user.getName() : "User",
                rawToken);
        log.info("Resent email verification to: {}", email);
    }

    /* ---------- Helpers ---------- */

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
