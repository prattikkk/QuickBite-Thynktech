package com.quickbite.auth.service;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RefreshRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.UserProfileDTO;
import com.quickbite.auth.exception.AuthException;
import com.quickbite.auth.exception.InvalidTokenException;
import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.auth.entity.TokenStore;
import com.quickbite.auth.repository.TokenStoreRepository;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.sms.service.SmsDispatchService;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for handling authentication operations.
 * Manages user registration, login, token refresh, and logout.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenStoreRepository tokenStoreRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final VendorRepository vendorRepository;
    private final PasswordResetService passwordResetService;
    private final com.quickbite.email.service.EmailDispatchService emailDispatchService;
    private final SmsDispatchService smsDispatchService;

    /**
     * Register a new user.
     *
     * @param request registration details
     * @return authentication response with tokens
     * @throws AuthException if email already exists or role not found
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered: " + request.getEmail());
        }

        // Determine role (default to CUSTOMER)
        String roleName = request.getRoleName() != null ? request.getRoleName() : "CUSTOMER";
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AuthException("Role not found: " + roleName));

        // Create user
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .role(role)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Auto-create a Vendor entity for VENDOR users so they can manage their restaurant
        if ("VENDOR".equalsIgnoreCase(roleName)) {
            Vendor vendor = Vendor.builder()
                    .user(savedUser)
                    .name(savedUser.getName() + "'s Restaurant")
                    .description("Welcome to my restaurant!")
                    .active(true)
                    .build();
            vendorRepository.save(vendor);
            log.info("Auto-created vendor profile for user: {}", savedUser.getEmail());
        }

        // Create email verification token (in production, this would be emailed)
        String verificationToken = passwordResetService.createEmailVerificationToken(savedUser.getId());
        log.info("Email verification token created for user: {} token: {}", savedUser.getEmail(), verificationToken);

        // Dispatch welcome + verification emails (async, fire-and-forget)
        try {
            emailDispatchService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
            emailDispatchService.sendEmailVerification(savedUser.getEmail(), savedUser.getName(), verificationToken);
        } catch (Exception e) {
            log.warn("Email dispatch failed during registration for {}: {}", savedUser.getEmail(), e.getMessage());
        }

        // Dispatch registration SMS if phone number was provided
        if (savedUser.getPhone() != null && !savedUser.getPhone().isBlank()) {
            try {
                String verifyLink = emailDispatchService.buildVerifyLink(verificationToken);
                smsDispatchService.sendRegistrationSms(savedUser.getPhone(), savedUser.getName(), verifyLink);
            } catch (Exception e) {
                log.warn("SMS dispatch failed during registration for {}: {}", savedUser.getEmail(), e.getMessage());
            }
        }

        // Generate tokens
        return generateAuthResponse(savedUser);
    }

    /**
     * Authenticate user and generate tokens.
     *
     * @param request login credentials
     * @return authentication response with tokens
     * @throws AuthException if authentication fails or account is locked
     */
    @Transactional(noRollbackFor = AuthException.class)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        // Load user first to check lockout status
        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        // Check if account is locked
        if (user != null && user.getAccountLockedUntil() != null 
                && user.getAccountLockedUntil().isAfter(OffsetDateTime.now())) {
            long minutesRemaining = java.time.Duration.between(
                OffsetDateTime.now(), user.getAccountLockedUntil()
            ).toMinutes();
            log.warn("Account locked for user: {} - {} minutes remaining", request.getEmail(), minutesRemaining);
            throw new AuthException("Account is temporarily locked due to too many failed login attempts. Please try again in " + minutesRemaining + " minutes.");
        }

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Reload user (in case it was null)
            if (user == null) {
                user = userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new AuthException("User not found"));
            }

            if (!user.getActive()) {
                throw new AuthException("User account is deactivated");
            }

            // Reset failed login attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.setFailedLoginAttempts(0);
                user.setAccountLockedUntil(null);
                userRepository.save(user);
                log.info("Reset failed login attempts for user: {}", user.getEmail());
            }

            log.info("User authenticated successfully: {}", user.getEmail());

            // Generate tokens
            return generateAuthResponse(user);

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", request.getEmail());
            
            // Increment failed login attempts
            if (user != null) {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                
                // Lock account for 15 minutes after 5 failed attempts
                if (attempts >= 5) {
                    user.setAccountLockedUntil(OffsetDateTime.now().plusMinutes(15));
                    userRepository.save(user);
                    log.warn("Account locked for user: {} after {} failed attempts", request.getEmail(), attempts);
                    throw new AuthException("Account locked due to too many failed login attempts. Please try again in 15 minutes.");
                } else {
                    userRepository.save(user);
                    log.warn("Failed login attempt {} for user: {}", attempts, request.getEmail());
                }
            }
            
            throw new AuthException("Invalid email or password", e);
        }
    }

    /**
     * Refresh access token using refresh token.
     *
     * @param request refresh token
     * @return new auth response with new access token
     * @throws InvalidTokenException if refresh token is invalid or revoked
     */
    @Transactional
    public AuthResponse refreshToken(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        
        log.debug("Refreshing access token");

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        // Verify it's a refresh token
        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        // Get token hash
        String tokenHash = hashToken(refreshToken);

        // Check if token exists and is not revoked
        TokenStore tokenStore = tokenStoreRepository.findByTokenHashAndRevoked(tokenHash, false)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found or revoked"));

        // Check if token is expired
        if (tokenStore.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Get user
        User user = tokenStore.getUser();
        if (!user.getActive()) {
            throw new AuthException("User account is deactivated");
        }

        // Generate new access token (keep same refresh token)
        // --- Refresh Token Rotation (Phase 4 — Security) ---
        // Revoke the old refresh token (one-time use)
        tokenStore.setRevoked(true);
        tokenStore.setRevokedAt(OffsetDateTime.now());
        tokenStoreRepository.save(tokenStore);

        // Generate both a new access token AND a new refresh token
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getName()
        );
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );
        storeRefreshToken(user, newRefreshToken);

        log.info("Token rotation completed for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().getName())
                .build();
    }

    /**
     * Get the current user's profile by their principal name (UUID string).
     *
     * @param principalName the UUID string from Authentication.getName()
     * @return user profile DTO
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getCurrentUserProfile(String principalName) {
        UUID userId = UUID.fromString(principalName);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getName())
                .phone(user.getPhone())
                .role(user.getRole().getName())
                .status(user.getActive() ? "ACTIVE" : "INACTIVE")
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Logout user by revoking refresh token.
     *
     * @param refreshToken refresh token to revoke
     */
    @Transactional
    public void logout(String refreshToken) {
        log.info("Logout request received");

        try {
            String tokenHash = hashToken(refreshToken);

            tokenStoreRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                token.setRevokedAt(OffsetDateTime.now());
                tokenStoreRepository.save(token);
                log.info("Refresh token revoked for user: {}", token.getUser().getEmail());
            });
        } catch (Exception e) {
            log.warn("Failed to revoke token: {}", e.getMessage());
            // Don't throw exception, logout should succeed even if token is invalid
        }
    }

    /**
     * Generate authentication response with access and refresh tokens.
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().getName()
        );

        String refreshToken = jwtTokenProvider.generateRefreshToken(
                user.getId(),
                user.getEmail()
        );

        // Store refresh token in database (hashed)
        storeRefreshToken(user, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().getName())
                .build();
    }

    /**
     * Store refresh token in database (hashed).
     */
    private void storeRefreshToken(User user, String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        // Calculate expiration (7 days from now)
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(7);

        TokenStore tokenStore = TokenStore.builder()
                .user(user)
                .tokenHash(tokenHash)
                .tokenType("REFRESH")
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        tokenStoreRepository.save(tokenStore);
        log.debug("Refresh token stored for user: {}", user.getEmail());
    }

    /**
     * Hash token using SHA-256 (deterministic for lookup).
     * Unlike BCrypt, SHA-256 produces the same hash for the same input.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Clean up expired tokens (scheduled task).
     */
    @Transactional
    public void cleanupExpiredTokens() {
        OffsetDateTime now = OffsetDateTime.now();
        int deleted = tokenStoreRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Cleaned up {} expired tokens", deleted);
        }
    }
}
