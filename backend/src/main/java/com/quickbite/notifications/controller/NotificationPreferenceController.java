package com.quickbite.notifications.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.notifications.dto.NotificationPreferenceDTO;
import com.quickbite.notifications.service.NotificationPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Manage notification channel toggles")
@SecurityRequirement(name = "bearerAuth")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    @Operation(summary = "Get notification preferences", description = "Get the current user's notification preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> getPreferences(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationPreferenceDTO prefs = preferenceService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved", prefs));
    }

    @PutMapping
    @Operation(summary = "Update notification preferences", description = "Update the current user's notification channel toggles")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> updatePreferences(
            @RequestBody NotificationPreferenceDTO request,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        NotificationPreferenceDTO prefs = preferenceService.updatePreferences(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", prefs));
    }
}
