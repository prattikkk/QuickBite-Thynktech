package com.quickbite.auth.controller;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.ForgotPasswordRequest;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RefreshRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.dto.ResetPasswordRequest;
import com.quickbite.auth.dto.UserProfileDTO;
import com.quickbite.auth.dto.VerifyEmailRequest;
import com.quickbite.auth.service.AuthService;
import com.quickbite.auth.service.PasswordResetService;
import com.quickbite.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for authentication endpoints.
 * Handles user registration, login, token refresh, and logout.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * Register a new user.
     * POST /api/auth/register
     * Sets refresh token as HttpOnly cookie for security.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        log.info("Registration request received for email: {}", request.getEmail());
        
        AuthResponse authResponse = authService.register(request);
        
        // Set refresh token as HttpOnly, Secure, SameSite=Strict cookie
        if (authResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/api/auth");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);
            
            // Remove refresh token from response body
            authResponse.setRefreshToken(null);
        }
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * Authenticate user and generate tokens.
     * POST /api/auth/login
     * Sets refresh token as HttpOnly cookie for security.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        log.info("Login request received for email: {}", request.getEmail());
        
        AuthResponse authResponse = authService.login(request);
        
        // Set refresh token as HttpOnly, Secure, SameSite=Strict cookie
        if (authResponse.getRefreshToken() != null) {
            Cookie refreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true); // HTTPS only (set false for local dev if needed)
            refreshCookie.setPath("/api/auth");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);
            
            // Remove refresh token from response body (security best practice)
            authResponse.setRefreshToken(null);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Refresh access token using refresh token from HttpOnly cookie.
     * POST /api/auth/refresh
     * Reads refresh token from cookie, generates new tokens, sets new cookie.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshRequest requestBody,
            HttpServletResponse response) {
        log.debug("Token refresh request received");
        
        // Support both cookie (new) and body (legacy) for gradual migration
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && requestBody != null && requestBody.getRefreshToken() != null) {
            refreshToken = requestBody.getRefreshToken();
            log.warn("Refresh token sent in body (legacy). Use HttpOnly cookie instead.");
        }
        
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh token is missing", null));
        }
        
        RefreshRequest request = new RefreshRequest(refreshToken);
        AuthResponse authResponse = authService.refreshToken(request);
        
        // Set new refresh token as HttpOnly cookie
        if (authResponse.getRefreshToken() != null) {
            Cookie newRefreshCookie = new Cookie("refreshToken", authResponse.getRefreshToken());
            newRefreshCookie.setHttpOnly(true);
            newRefreshCookie.setSecure(true);
            newRefreshCookie.setPath("/api/auth");
            newRefreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            newRefreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(newRefreshCookie);
            
            // Remove refresh token from response body
            authResponse.setRefreshToken(null);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authResponse));
    }

    /**
     * Logout user by revoking refresh token and clearing cookie.
     * POST /api/auth/logout
     * Reads refresh token from cookie, revokes it, clears cookie.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshRequest requestBody,
            HttpServletResponse response) {
        log.info("Logout request received");
        
        // Support both cookie (new) and body (legacy)
        String refreshToken = refreshTokenFromCookie;
        if (refreshToken == null && requestBody != null && requestBody.getRefreshToken() != null) {
            refreshToken = requestBody.getRefreshToken();
        }
        
        // Revoke token if present
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        
        // Clear refresh token cookie
        Cookie clearCookie = new Cookie("refreshToken", "");
        clearCookie.setHttpOnly(true);
        clearCookie.setSecure(true);
        clearCookie.setPath("/api/auth");
        clearCookie.setMaxAge(0); // Delete cookie
        clearCookie.setAttribute("SameSite", "Strict");
        response.addCookie(clearCookie);
        
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    /**
     * Get the currently authenticated user's profile.
     * GET /api/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDTO>> getCurrentUser(Authentication authentication) {
        log.debug("Get current user request");

        UserProfileDTO profile = authService.getCurrentUserProfile(authentication.getName());

        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", profile));
    }

    /**
     * Request a password reset link.
     * POST /api/auth/forgot-password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());

        String token = passwordResetService.requestPasswordReset(request);

        // Always return success to prevent email enumeration.
        // In production the token would be emailed; here we return it for testing.
        if (token != null) {
            return ResponseEntity.ok(ApiResponse.success(
                    "If an account with that email exists, a reset link has been sent.", token));
        }
        return ResponseEntity.ok(ApiResponse.success(
                "If an account with that email exists, a reset link has been sent.", null));
    }

    /**
     * Reset password using the token.
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received");

        passwordResetService.resetPassword(request);

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    /**
     * Verify email address using the token.
     * POST /api/auth/verify-email
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        log.info("Email verification request received");

        passwordResetService.verifyEmail(request);

        return ResponseEntity.ok(ApiResponse.success("Email verified successfully", null));
    }

    /**
     * Resend email verification link.
     * POST /api/auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Email is required", null));
        }
        log.info("Resend verification request for: {}", email);

        passwordResetService.resendEmailVerification(email);

        return ResponseEntity.ok(ApiResponse.success("If the email exists, a new verification link has been sent.", null));
    }
}
