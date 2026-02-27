package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for authentication response containing tokens and user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn; // seconds until access token expires
    
    // User information
    private UUID userId;
    private String email;
    private String name;
    private String role;
    private Boolean emailVerified;
}
