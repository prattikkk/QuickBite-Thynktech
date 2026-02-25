package com.quickbite.users.controller;

import com.quickbite.auth.entity.TokenStore;
import com.quickbite.auth.repository.TokenStoreRepository;
import com.quickbite.common.dto.ApiResponse;
import com.quickbite.favorites.repository.FavoriteRepository;
import com.quickbite.notifications.repository.NotificationRepository;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.users.entity.Address;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Base64;

/**
 * User account management controller.
 * Phase 4 — GDPR erasure; Phase 5 — profile management + data portability.
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Account", description = "User profile, GDPR erasure & data export")
@SecurityRequirement(name = "bearerAuth")
public class UserAccountController {

    private final UserRepository userRepository;
    private final TokenStoreRepository tokenStoreRepository;
    private final OrderRepository orderRepository;
    private final FavoriteRepository favoriteRepository;
    private final NotificationRepository notificationRepository;

    // ── GET /api/users/me — view profile ──────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get my profile", description = "Returns the authenticated user's profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyProfile(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> profile = buildProfileMap(user);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", profile));
    }

    // ── POST /api/users/me/avatar — upload profile picture ──────────────

    @PostMapping(value = "/me/avatar", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER')")
    @Operation(summary = "Upload avatar", description = "Upload a profile picture (max 2 MB, jpeg/png/webp)")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAvatar(
            @RequestParam("photo") MultipartFile photo,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        if (photo.isEmpty()) throw new RuntimeException("Photo file is empty");

        long maxBytes = 2 * 1024 * 1024; // 2 MB
        if (photo.getSize() > maxBytes) throw new RuntimeException("Photo must be under 2 MB");

        String ct = photo.getContentType();
        if (ct == null || (!ct.startsWith("image/jpeg") && !ct.startsWith("image/png") && !ct.startsWith("image/webp")))
            throw new RuntimeException("Only JPEG, PNG, or WEBP images are supported");

        try {
            byte[] bytes = photo.getBytes();
            String b64 = Base64.getEncoder().encodeToString(bytes);
            String dataUrl = "data:" + ct + ";base64," + b64;

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setAvatarUrl(dataUrl);
            userRepository.save(user);

            log.info("Avatar uploaded for user {}", userId);
            Map<String, Object> profile = buildProfileMap(user);
            return ResponseEntity.ok(ApiResponse.success("Avatar uploaded", profile));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    // ── PUT /api/users/me — update name / phone ──────────────────────

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER')")
    @Operation(summary = "Update my profile", description = "Update name and/or phone")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateMyProfile(
            @Valid @RequestBody ProfileUpdateDTO dto,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());

        userRepository.save(user);
        log.info("User {} profile updated", userId);

        Map<String, Object> profile = buildProfileMap(user);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    // ── GET /api/users/me/export — GDPR data portability (Art. 20) ───

    @GetMapping("/me/export")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER')")
    @Operation(summary = "Export my data", description = "GDPR data portability: export all personal data as JSON")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportMyData(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        log.info("GDPR data export request for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("profile", buildProfileMap(user));

        // Addresses
        List<Map<String, Object>> addresses = user.getAddresses().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("line1", a.getLine1());
            m.put("line2", a.getLine2());
            m.put("city", a.getCity());
            m.put("state", a.getState());
            m.put("postal", a.getPostal());
            m.put("country", a.getCountry());
            m.put("isDefault", a.getIsDefault());
            return m;
        }).collect(Collectors.toList());
        export.put("addresses", addresses);

        // Orders (first 500)
        var orders = orderRepository.findByCustomerId(userId, PageRequest.of(0, 500));
        List<Map<String, Object>> orderList = orders.getContent().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("status", o.getStatus().name());
            m.put("totalCents", o.getTotalCents());
            m.put("createdAt", o.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        export.put("orders", orderList);

        // Favorites
        var favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> favList = favorites.stream().map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("vendorId", f.getVendor() != null ? f.getVendor().getId() : null);
            m.put("createdAt", f.getCreatedAt());
            return m;
        }).collect(Collectors.toList());
        export.put("favorites", favList);

        export.put("exportedAt", OffsetDateTime.now());

        return ResponseEntity.ok(ApiResponse.success("Data exported", export));
    }

    // ── DELETE /api/users/me — GDPR erasure (Art. 17) ────────────────

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

    // ── Helpers ──────────────────────────────────────────────────────

    private Map<String, Object> buildProfileMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("email", user.getEmail());
        m.put("name", user.getName());
        m.put("phone", user.getPhone());
        m.put("role", user.getRole().getName());
        m.put("active", user.getActive());
        m.put("emailVerified", user.getEmailVerified() != null && user.getEmailVerified());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("createdAt", user.getCreatedAt());
        return m;
    }

    // ── DTO ─────────────────────────────────────────────────────────

    @Data
    public static class ProfileUpdateDTO {
        @Size(min = 1, max = 255, message = "Name must be 1-255 characters")
        private String name;

        @Size(max = 20, message = "Phone must be at most 20 characters")
        private String phone;
    }
}
