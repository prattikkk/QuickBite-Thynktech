package com.quickbite.common.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin management controller — user ban/activate, vendor approval.
 * Phase 5 — governance & moderation capabilities.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "User & vendor administration")
@SecurityRequirement(name = "bearerAuth")
public class AdminManagementController {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    // ── Users ────────────────────────────────────────────────────────

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users", description = "Paginated user list with optional name search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByNameContainingIgnoreCase(search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        List<Map<String, Object>> content = users.getContent().stream()
                .filter(u -> role == null || (u.getRole() != null && u.getRole().getName().equalsIgnoreCase(role)))
                .map(this::userSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", users.getNumber());
        result.put("size", users.getSize());
        result.put("totalElements", users.getTotalElements());
        result.put("totalPages", users.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Users retrieved", result));
    }

    @PutMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ban or activate user", description = "Set user active status (ban = active:false)")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable UUID userId,
            @RequestParam boolean active) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Prevent admin from banning themselves
        if ("ADMIN".equals(user.getRole().getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Cannot change admin user status"));
        }

        user.setActive(active);
        userRepository.save(user);

        String action = active ? "activated" : "banned";
        log.info("Admin {} user {}: {}", action, userId, user.getEmail());

        return ResponseEntity.ok(ApiResponse.success("User " + action, userSummary(user)));
    }

    // ── Vendors ──────────────────────────────────────────────────────

    @GetMapping("/vendors")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all vendors", description = "Paginated vendor list including inactive")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Vendor> vendors = vendorRepository.findAll(pageable);

        List<Map<String, Object>> content = vendors.getContent().stream()
                .map(this::vendorSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("page", vendors.getNumber());
        result.put("size", vendors.getSize());
        result.put("totalElements", vendors.getTotalElements());
        result.put("totalPages", vendors.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Vendors retrieved", result));
    }

    @PutMapping("/vendors/{vendorId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve or deactivate vendor", description = "Approve (active:true) or deactivate (active:false) a vendor")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveVendor(
            @PathVariable UUID vendorId,
            @RequestParam boolean active) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        vendor.setActive(active);
        vendorRepository.save(vendor);

        String action = active ? "approved" : "deactivated";
        log.info("Admin {} vendor {}: {}", action, vendorId, vendor.getName());

        return ResponseEntity.ok(ApiResponse.success("Vendor " + action, vendorSummary(vendor)));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Map<String, Object> userSummary(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("phone", u.getPhone());
        m.put("role", u.getRole() != null ? u.getRole().getName() : null);
        m.put("active", u.getActive());
        m.put("createdAt", u.getCreatedAt());
        return m;
    }

    private Map<String, Object> vendorSummary(Vendor v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("name", v.getName());
        m.put("description", v.getDescription());
        m.put("active", v.getActive());
        m.put("rating", v.getRating());
        m.put("userId", v.getUser() != null ? v.getUser().getId() : null);
        m.put("createdAt", v.getCreatedAt());
        return m;
    }
}
