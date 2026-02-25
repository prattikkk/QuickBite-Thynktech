package com.quickbite.push.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.push.dto.RegisterDeviceRequest;
import com.quickbite.push.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final PushNotificationService pushNotificationService;

    /**
     * Register a device token for push notifications.
     * POST /api/devices/register
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> registerDevice(
            @Valid @RequestBody RegisterDeviceRequest request,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        pushNotificationService.registerDevice(userId, request.getToken(), request.getPlatform());

        return ResponseEntity.ok(ApiResponse.success("Device registered", null));
    }

    /**
     * Unregister a device token.
     * DELETE /api/devices/{token}
     */
    @DeleteMapping("/{token}")
    public ResponseEntity<ApiResponse<Void>> unregisterDevice(
            @PathVariable String token,
            Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());
        pushNotificationService.unregisterDevice(userId, token);

        return ResponseEntity.ok(ApiResponse.success("Device unregistered", null));
    }
}
