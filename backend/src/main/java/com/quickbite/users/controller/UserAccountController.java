package com.quickbite.users.controller;

import com.quickbite.auth.entity.TokenStore;
import com.quickbite.auth.repository.TokenStoreRepository;
import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * User account management controller.
 * Phase 4 — Security: GDPR right to erasure (Art. 17).
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "User account management (delete / GDPR)")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UserRepository userRepository;
    private final TokenStoreRepository tokenStoreRepository;

    /**
     * Delete (anonymize) the current user's account.
     * PII is scrubbed: email → deleted-{uuid}@anon, name → "Deleted User", phone → null.
     * Tokens are revoked. Account is deactivated.
     */
    @DeleteMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER')")
    @Operation(summary = "Delete my account", description = "GDPR erasure: anonymize PII and deactivate account")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        log.info("GDPR deletion request for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Anonymize PII
        user.setEmail("deleted-" + userId + "@anon.quickbite.local");
        user.setName("Deleted User");
        user.setPhone(null);
        user.setPasswordHash("DELETED");
        user.setActive(false);

        userRepository.save(user);

        // Revoke all tokens
        tokenStoreRepository.revokeAllUserTokens(userId, OffsetDateTime.now());

        log.info("User {} account anonymized and deactivated (GDPR)", userId);

        return ResponseEntity.ok(ApiResponse.success("Account deleted and PII anonymized", null));
    }
}
