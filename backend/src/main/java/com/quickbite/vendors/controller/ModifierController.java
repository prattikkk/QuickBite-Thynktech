package com.quickbite.vendors.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.vendors.dto.*;
import com.quickbite.vendors.service.ModifierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for modifier group and modifier CRUD operations.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Modifiers", description = "Menu item modifier management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ModifierController {

    private final ModifierService modifierService;

    // ---- Modifier Group endpoints ----

    /**
     * List modifier groups with modifiers for a menu item.
     */
    @GetMapping("/api/menu-items/{itemId}/modifiers")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "List modifier groups", description = "Get modifier groups and modifiers for a menu item")
    public ResponseEntity<ApiResponse<List<ModifierGroupDTO>>> getModifierGroups(
            @PathVariable UUID itemId
    ) {
        log.debug("Getting modifier groups for menu item {}", itemId);
        List<ModifierGroupDTO> groups = modifierService.getModifierGroups(itemId);
        return ResponseEntity.ok(ApiResponse.success("Modifier groups retrieved", groups));
    }

    /**
     * Create a modifier group for a menu item.
     */
    @PostMapping("/api/menu-items/{itemId}/modifier-groups")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Create modifier group", description = "Create a new modifier group for a menu item")
    public ResponseEntity<ApiResponse<ModifierGroupDTO>> createModifierGroup(
            @PathVariable UUID itemId,
            @Valid @RequestBody ModifierGroupCreateDTO dto
    ) {
        log.debug("Creating modifier group '{}' for menu item {}", dto.getName(), itemId);
        ModifierGroupDTO created = modifierService.createModifierGroup(itemId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Modifier group created", created));
    }

    /**
     * Update a modifier group.
     */
    @PutMapping("/api/modifier-groups/{groupId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update modifier group", description = "Update an existing modifier group")
    public ResponseEntity<ApiResponse<ModifierGroupDTO>> updateModifierGroup(
            @PathVariable UUID groupId,
            @Valid @RequestBody ModifierGroupCreateDTO dto
    ) {
        log.debug("Updating modifier group {}", groupId);
        ModifierGroupDTO updated = modifierService.updateModifierGroup(groupId, dto);
        return ResponseEntity.ok(ApiResponse.success("Modifier group updated", updated));
    }

    /**
     * Delete a modifier group and its modifiers.
     */
    @DeleteMapping("/api/modifier-groups/{groupId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete modifier group", description = "Delete a modifier group and all its modifiers")
    public ResponseEntity<ApiResponse<Void>> deleteModifierGroup(
            @PathVariable UUID groupId
    ) {
        log.debug("Deleting modifier group {}", groupId);
        modifierService.deleteModifierGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success("Modifier group deleted", null));
    }

    // ---- Modifier endpoints ----

    /**
     * Add a modifier to a group.
     */
    @PostMapping("/api/modifier-groups/{groupId}/modifiers")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Add modifier", description = "Add a modifier to a modifier group")
    public ResponseEntity<ApiResponse<ModifierDTO>> addModifier(
            @PathVariable UUID groupId,
            @Valid @RequestBody ModifierCreateDTO dto
    ) {
        log.debug("Adding modifier '{}' to group {}", dto.getName(), groupId);
        ModifierDTO created = modifierService.addModifier(groupId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Modifier created", created));
    }

    /**
     * Update a modifier.
     */
    @PutMapping("/api/modifiers/{modifierId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Update modifier", description = "Update an existing modifier")
    public ResponseEntity<ApiResponse<ModifierDTO>> updateModifier(
            @PathVariable UUID modifierId,
            @Valid @RequestBody ModifierCreateDTO dto
    ) {
        log.debug("Updating modifier {}", modifierId);
        ModifierDTO updated = modifierService.updateModifier(modifierId, dto);
        return ResponseEntity.ok(ApiResponse.success("Modifier updated", updated));
    }

    /**
     * Delete a modifier.
     */
    @DeleteMapping("/api/modifiers/{modifierId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    @Operation(summary = "Delete modifier", description = "Delete a modifier")
    public ResponseEntity<ApiResponse<Void>> deleteModifier(
            @PathVariable UUID modifierId
    ) {
        log.debug("Deleting modifier {}", modifierId);
        modifierService.deleteModifier(modifierId);
        return ResponseEntity.ok(ApiResponse.success("Modifier deleted", null));
    }
}
