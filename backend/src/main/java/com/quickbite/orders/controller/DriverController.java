package com.quickbite.orders.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.delivery.entity.DeliveryStatus;
import com.quickbite.delivery.repository.DeliveryStatusRepository;
import com.quickbite.orders.driver.DriverProfileDTO;
import com.quickbite.orders.driver.DriverProfileService;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.orders.service.OrderService;
import com.quickbite.websocket.OrderUpdatePublisher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Driver-specific REST controller.
 * Phase 5 — provides dedicated driver endpoints beyond generic OrderController.
 */
@Slf4j
@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Drivers", description = "Driver-specific delivery endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final DeliveryStatusRepository deliveryStatusRepository;
    private final DriverProfileService driverProfileService;
    private final OrderUpdatePublisher orderUpdatePublisher;

    /**
     * Get available (unassigned) READY orders a driver can pick up.
     */
    @GetMapping("/available-orders")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Available orders", description = "List unassigned READY orders for pickup")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableOrders(Authentication auth) {
        UUID driverId = extractUserId(auth);
        log.info("Driver {} fetching available orders", driverId);

        List<Order> ready = orderRepository.findByDriverIsNullAndStatus(OrderStatus.READY);
        List<Map<String, Object>> list = ready.stream().map(this::orderSummary).collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Available orders", list));
    }

    /**
     * Accept / claim an available order.
     */
    @PostMapping("/orders/{orderId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Accept order", description = "Driver claims an available order for delivery")
    @Transactional
    public ResponseEntity<ApiResponse<OrderResponseDTO>> acceptOrder(
            @PathVariable UUID orderId, Authentication auth) {

        UUID driverId = extractUserId(auth);
        log.info("Driver {} accepting order {}", driverId, orderId);

        // Use the existing manual-assign flow
        OrderResponseDTO dto = orderService.assignDriverManually(orderId, driverId, driverId);
        return ResponseEntity.ok(ApiResponse.success("Order accepted", dto));
    }

    /**
     * Get the driver's current active delivery (first non-terminal assigned order).
     */
    @GetMapping("/active-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Active delivery", description = "Get the driver's current in-progress delivery")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveDelivery(Authentication auth) {
        UUID driverId = extractUserId(auth);

        // Active statuses for a driver: ASSIGNED, PICKED_UP, ENROUTE
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.ENROUTE);

        Optional<Order> active = activeStatuses.stream()
                .flatMap(s -> orderRepository.findByDriverIdAndStatus(driverId, s).stream())
                .findFirst();

        if (active.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No active delivery", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Active delivery", orderSummary(active.get())));
    }

    /**
     * Update driver location for current delivery.
     */
    @PutMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update location", description = "Report driver GPS location for active delivery")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @RequestBody LocationUpdateDTO dto, Authentication auth) {

        UUID driverId = extractUserId(auth);
        log.info("Driver {} location update: lat={}, lng={}", driverId, dto.getLat(), dto.getLng());

        // Find active order for this driver
        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.ASSIGNED, OrderStatus.PICKED_UP, OrderStatus.ENROUTE);

        Optional<Order> activeOrder = activeStatuses.stream()
                .flatMap(s -> orderRepository.findByDriverIdAndStatus(driverId, s).stream())
                .findFirst();

        // Always update driver profile GPS (trackable when online)
        if (dto.getLat() != null && dto.getLng() != null) {
            driverProfileService.updateLocation(driverId, dto.getLat(), dto.getLng());
        }

        if (activeOrder.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("Location updated (no active delivery)", null));
        }

        // Record a location-only delivery status entry
        DeliveryStatus loc = DeliveryStatus.builder()
                .order(activeOrder.get())
                .status(activeOrder.get().getStatus())
                .changedByUserId(driverId)
                .note("Location update")
                .locationLat(dto.getLat() != null ? BigDecimal.valueOf(dto.getLat()) : null)
                .locationLng(dto.getLng() != null ? BigDecimal.valueOf(dto.getLng()) : null)
                .build();
        deliveryStatusRepository.save(loc);

        // Broadcast location to WebSocket subscribers
        if (dto.getLat() != null && dto.getLng() != null) {
            orderUpdatePublisher.publishDriverLocation(driverId, dto.getLat(), dto.getLng(),
                    activeOrder.get().getId());
        }

        return ResponseEntity.ok(ApiResponse.success("Location updated", null));
    }

    /**
     * Get delivery history for the current driver.
     */
    @GetMapping("/delivery-history")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Delivery history", description = "List past completed deliveries")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDeliveryHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        UUID driverId = extractUserId(auth);

        var pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        var orders = orderRepository.findByDriverId(driverId, pageable);

        List<Map<String, Object>> list = orders.getContent().stream()
                .map(this::orderSummary)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", list);
        result.put("page", orders.getNumber());
        result.put("size", orders.getSize());
        result.put("totalElements", orders.getTotalElements());
        result.put("totalPages", orders.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success("Delivery history", list));
    }

    // ── Profile endpoints ────────────────────────────────────────────

    /**
     * Get the current driver's profile (vehicle, stats, online status).
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Get driver profile", description = "Get vehicle info, stats, online/offline status")
    public ResponseEntity<ApiResponse<DriverProfileDTO>> getProfile(Authentication auth) {
        UUID driverId = extractUserId(auth);
        DriverProfileDTO profile = driverProfileService.getProfileDTO(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver profile", profile));
    }

    /**
     * Update driver profile (vehicle type, license plate).
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Update driver profile", description = "Update vehicle type and license plate")
    public ResponseEntity<ApiResponse<DriverProfileDTO>> updateProfile(
            @RequestBody ProfileUpdateDTO dto, Authentication auth) {
        UUID driverId = extractUserId(auth);
        DriverProfileDTO profile = driverProfileService.updateProfile(
                driverId, dto.getVehicleType(), dto.getLicensePlate());
        return ResponseEntity.ok(ApiResponse.success("Profile updated", profile));
    }

    /**
     * Toggle driver online/offline status.
     */
    @PutMapping("/status")
    @PreAuthorize("hasRole('DRIVER')")
    @Operation(summary = "Toggle online status", description = "Set driver online or offline for receiving orders")
    public ResponseEntity<ApiResponse<DriverProfileDTO>> toggleStatus(
            @RequestBody StatusUpdateRequest dto, Authentication auth) {
        UUID driverId = extractUserId(auth);
        DriverProfileDTO profile = driverProfileService.toggleOnlineStatus(driverId, dto.isOnline());
        return ResponseEntity.ok(ApiResponse.success(
                dto.isOnline() ? "You are now online" : "You are now offline", profile));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private Map<String, Object> orderSummary(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getId());
        m.put("status", o.getStatus().name());
        m.put("totalCents", o.getTotalCents());
        m.put("vendorId", o.getVendor() != null ? o.getVendor().getId() : null);
        m.put("vendorName", o.getVendor() != null ? o.getVendor().getName() : null);
        m.put("customerId", o.getCustomer() != null ? o.getCustomer().getId() : null);
        if (o.getDeliveryAddress() != null) {
            Map<String, Object> addr = new LinkedHashMap<>();
            addr.put("line1", o.getDeliveryAddress().getLine1());
            addr.put("city", o.getDeliveryAddress().getCity());
            addr.put("state", o.getDeliveryAddress().getState());
            addr.put("postal", o.getDeliveryAddress().getPostal());
            m.put("deliveryAddress", addr);
        }
        m.put("createdAt", o.getCreatedAt());
        return m;
    }

    private UUID extractUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    // ── DTOs ─────────────────────────────────────────────────────────

    @Data
    public static class LocationUpdateDTO {
        private Double lat;
        private Double lng;
    }

    @Data
    public static class ProfileUpdateDTO {
        private String vehicleType;
        private String licensePlate;
    }

    @Data
    public static class StatusUpdateRequest {
        private boolean online;
    }
}
