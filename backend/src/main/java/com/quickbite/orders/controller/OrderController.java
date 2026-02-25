package com.quickbite.orders.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.delivery.entity.DeliveryStatus;
import com.quickbite.orders.dto.OrderCreateDTO;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.dto.StatusUpdateDTO;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.service.OrderService;
import com.quickbite.vendors.repository.VendorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for order operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final VendorRepository vendorRepository;

    /**
     * Create a new order (CUSTOMER only).
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create order", description = "Create a new order from cart items")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> createOrder(
            @Valid @RequestBody OrderCreateDTO dto,
            Authentication authentication
    ) {
        UUID customerId = extractUserId(authentication);
        log.info("Creating order for customer: {}", customerId);

        OrderResponseDTO order = orderService.createOrder(dto, customerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    /**
     * Get order by ID.
     * Accessible to customer, vendor, driver, or admin.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get order", description = "Get order details by ID")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> getOrder(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        OrderResponseDTO order = orderService.getOrder(id, userId);

        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", order));
    }

    /**
     * List orders with filters and pagination.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "List orders", description = "List orders with optional filters")
    public ResponseEntity<ApiResponse<Page<OrderResponseDTO>>> listOrders(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            Authentication authentication
    ) {
        // If not admin, restrict to own orders
        UUID userId = extractUserId(authentication);
        if (!hasRole(authentication, "ADMIN")) {
            if (hasRole(authentication, "CUSTOMER") && customerId == null) {
                customerId = userId;
            } else if (hasRole(authentication, "VENDOR") && vendorId == null) {
                vendorId = vendorRepository.findByUserId(userId)
                        .map(v -> v.getId())
                        .orElse(userId);
            } else if (hasRole(authentication, "DRIVER")) {
                // Driver sees only their assigned orders
                Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
                Pageable pageable = PageRequest.of(page, size, sort);
                Page<OrderResponseDTO> driverOrders = orderService.listDriverOrders(userId, pageable);
                return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", driverOrders));
            }
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;

        Page<OrderResponseDTO> orders = orderService.listOrders(customerId, vendorId, orderStatus, pageable);

        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
    }

    /**
     * Update order status.
     * Role-based access: vendor can update until READY, driver after READY.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Update order status", description = "Update order status with validation")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> updateOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateDTO dto,
            Authentication authentication
    ) {
        UUID actorId = extractUserId(authentication);
        log.info("User {} updating order {} status to {}", actorId, id, dto.getStatus());

        OrderResponseDTO order = orderService.updateOrderStatus(id, dto, actorId);

        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", order));
    }

    /**
     * Vendor accepts order.
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Accept order", description = "Vendor accepts an order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> acceptOrder(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID vendorId = extractUserId(authentication);
        log.info("Vendor {} accepting order {}", vendorId, id);

        OrderResponseDTO order = orderService.acceptOrder(id, vendorId);

        return ResponseEntity.ok(ApiResponse.success("Order accepted successfully", order));
    }

    /**
     * Vendor rejects order.
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Reject order", description = "Vendor rejects an order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> rejectOrder(
            @PathVariable UUID id,
            @RequestParam String reason,
            Authentication authentication
    ) {
        UUID vendorId = extractUserId(authentication);
        log.info("Vendor {} rejecting order {}: {}", vendorId, id, reason);

        OrderResponseDTO order = orderService.rejectOrder(id, vendorId, reason);

        return ResponseEntity.ok(ApiResponse.success("Order rejected successfully", order));
    }

    /**
     * Get status history for an order.
     */
    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VENDOR', 'DRIVER', 'ADMIN')")
    @Operation(summary = "Get status history", description = "Get the status change history for an order")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStatusHistory(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID userId = extractUserId(authentication);
        List<DeliveryStatus> history = orderService.getStatusHistory(id, userId);
        List<Map<String, Object>> response = history.stream().map(ds -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ds.getId());
            m.put("status", ds.getStatus().name());
            m.put("note", ds.getNote());
            m.put("changedByUserId", ds.getChangedByUserId());
            m.put("changedAt", ds.getChangedAt());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Status history retrieved", response));
    }

    /**
     * Manually assign a driver to an order.
     */
    @PostMapping("/{id}/assign/{driverId}")
    @PreAuthorize("hasAnyRole('DRIVER', 'ADMIN')")
    @Operation(summary = "Assign driver", description = "Manually assign a driver to an order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> assignDriver(
            @PathVariable UUID id,
            @PathVariable UUID driverId,
            Authentication authentication
    ) {
        UUID actorId = extractUserId(authentication);
        log.info("User {} assigning driver {} to order {}", actorId, driverId, id);
        OrderResponseDTO order = orderService.assignDriverManually(id, driverId, actorId);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned successfully", order));
    }

    /**
     * Vendor assigns a specific runner (driver) to a READY order.
     * POST /api/orders/{id}/vendor/assign-driver?driverId=UUID
     */
    @PostMapping("/{id}/vendor/assign-driver")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Vendor assign driver", description = "Vendor manually assigns an online driver to a READY order")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> vendorAssignDriver(
            @PathVariable UUID id,
            @RequestParam UUID driverId,
            Authentication authentication
    ) {
        UUID actorId = extractUserId(authentication);
        log.info("Vendor {} assigning driver {} to order {}", actorId, driverId, id);
        OrderResponseDTO order = orderService.assignDriverManually(id, driverId, actorId);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned successfully", order));
    }

    /**
     * Reorder — create a new order by cloning items from a previous order.
     */
    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Reorder", description = "Create a new order from a previous order's items")
    public ResponseEntity<ApiResponse<OrderResponseDTO>> reorder(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UUID customerId = extractUserId(authentication);
        log.info("Customer {} reordering from order {}", customerId, id);
        OrderResponseDTO order = orderService.reorderFromPrevious(id, customerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reorder placed successfully", order));
    }

    // ========== Helper Methods ==========

    private UUID extractUserId(Authentication authentication) {
        // In production, extract from JWT claims or UserDetails
        // For now, parse from username (which is actually the user ID in our setup)
        String username = authentication.getName();
        try {
            return UUID.fromString(username);
        } catch (IllegalArgumentException e) {
            // Fallback: username might be email, need to look up user
            log.warn("Cannot extract UUID from username: {}", username);
            throw new IllegalStateException("Invalid user identification in token");
        }
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + role));
    }
}
