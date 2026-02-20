package com.quickbite.users.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.entity.Address;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.AddressRepository;
import com.quickbite.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing user delivery addresses.
 */
@Slf4j
@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Delivery address management")
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    /**
     * Get all addresses for the authenticated user.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "Get my addresses", description = "List all delivery addresses for the current user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyAddresses(Authentication authentication) {
        UUID userId = extractUserId(authentication);
        log.info("Getting addresses for user: {}", userId);

        List<Address> addresses = addressRepository.findByUserId(userId);
        List<Map<String, Object>> dtos = addresses.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved", dtos));
    }

    /**
     * Create a new address for the authenticated user.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "Create address", description = "Add a new delivery address")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createAddress(
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        Address address = Address.builder()
                .user(user)
                .line1((String) body.get("line1"))
                .line2((String) body.get("line2"))
                .city((String) body.get("city"))
                .state((String) body.get("state"))
                .postal((String) body.get("postal"))
                .country(body.getOrDefault("country", "India").toString())
                .isDefault(addresses(userId).isEmpty())
                .build();

        address = addressRepository.save(address);
        log.info("Address created: {} for user: {}", address.getId(), userId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created", toDTO(address)));
    }

    /**
     * Delete an address.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER', 'VENDOR', 'ADMIN')")
    @Operation(summary = "Delete address", description = "Remove a delivery address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + id));

        if (!address.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Not authorized to delete this address", null));
        }

        addressRepository.delete(address);
        log.info("Address deleted: {} for user: {}", id, userId);

        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    // ─── helpers ───────────────────────────────────────────────────────

    private List<Address> addresses(UUID userId) {
        return addressRepository.findByUserId(userId);
    }

    private Map<String, Object> toDTO(Address a) {
        return Map.of(
                "id", a.getId().toString(),
                "line1", a.getLine1() != null ? a.getLine1() : "",
                "line2", a.getLine2() != null ? a.getLine2() : "",
                "city", a.getCity() != null ? a.getCity() : "",
                "state", a.getState() != null ? a.getState() : "",
                "postal", a.getPostal() != null ? a.getPostal() : "",
                "country", a.getCountry() != null ? a.getCountry() : "India",
                "lat", a.getLat() != null ? a.getLat() : 0,
                "lng", a.getLng() != null ? a.getLng() : 0
        );
    }

    private UUID extractUserId(Authentication auth) {
        String username = auth.getName();
        try {
            return UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            log.warn("Cannot extract UUID from username: {}", username);
            throw new IllegalStateException("Invalid user identification in token");
        }
    }
}
