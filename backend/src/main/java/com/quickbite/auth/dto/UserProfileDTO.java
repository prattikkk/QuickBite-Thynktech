package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO returned by GET /api/auth/me — current user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String role;
    private String status;
    private OffsetDateTime createdAt;
}
