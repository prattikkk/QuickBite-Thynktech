package com.quickbite.common.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.common.feature.FeatureFlag;
import com.quickbite.common.feature.FeatureFlagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin API for managing feature flags at runtime.
 */
@RestController
@RequestMapping("/api/admin/feature-flags")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    /**
     * List all feature flags with their effective values.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getAllFlags() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.getAllFlags()));
    }

    /**
     * List all persisted flag entities with metadata.
     */
    @GetMapping("/details")
    public ResponseEntity<ApiResponse<List<FeatureFlag>>> getAllFlagDetails() {
        return ResponseEntity.ok(ApiResponse.success(featureFlagService.getAllFlagEntities()));
    }

    /**
     * Toggle a feature flag.
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiResponse<FeatureFlag>> toggleFlag(
            @PathVariable String key,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        String updatedBy = authentication != null ? authentication.getName() : "system";
        FeatureFlag flag = featureFlagService.toggle(key, enabled, updatedBy);
        return ResponseEntity.ok(ApiResponse.success(flag));
    }
}
