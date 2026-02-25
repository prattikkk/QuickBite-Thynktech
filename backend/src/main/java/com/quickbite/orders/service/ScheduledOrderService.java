package com.quickbite.orders.service;

import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.mapper.OrderMapper;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing scheduled (future) orders.
 * Validates scheduling windows, processes due orders on a fixed interval,
 * and provides queries for upcoming scheduled orders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledOrderService {

    private static final int MIN_SCHEDULE_AHEAD_MINUTES = 30;
    private static final int PROCESSING_WINDOW_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;
    private final OrderMapper orderMapper;

    /**
     * Validate that a scheduled time is acceptable:
     *  - at least 30 minutes in the future
     *  - within vendor operating hours (if openHours are configured)
     *
     * @param scheduledTime the desired scheduled time
     * @param vendorId      the target vendor UUID
     * @return true if the time is valid
     */
    @Transactional(readOnly = true)
    public boolean validateScheduledTime(OffsetDateTime scheduledTime, UUID vendorId) {
        if (scheduledTime == null) return false;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (scheduledTime.isBefore(now.plusMinutes(MIN_SCHEDULE_AHEAD_MINUTES))) {
            return false;
        }

        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor == null) return false;

        // Check vendor operating hours if configured
        if (vendor.getOpenHours() != null && !vendor.getOpenHours().isEmpty()) {
            String dayKey = scheduledTime.getDayOfWeek().name().toLowerCase();
            String hours = vendor.getOpenHours().get(dayKey);
            if (hours == null || hours.isBlank() || "closed".equalsIgnoreCase(hours.trim())) {
                return false;
            }
            // Parse "HH:mm-HH:mm" format
            try {
                String[] parts = hours.split("-");
                if (parts.length == 2) {
                    String[] openParts = parts[0].trim().split(":");
                    String[] closeParts = parts[1].trim().split(":");
                    int openHour = Integer.parseInt(openParts[0]);
                    int openMin = Integer.parseInt(openParts[1]);
                    int closeHour = Integer.parseInt(closeParts[0]);
                    int closeMin = Integer.parseInt(closeParts[1]);

                    int scheduledMinutes = scheduledTime.getHour() * 60 + scheduledTime.getMinute();
                    int openMinutes = openHour * 60 + openMin;
                    int closeMinutes = closeHour * 60 + closeMin;

                    if (scheduledMinutes < openMinutes || scheduledMinutes > closeMinutes) {
                        return false;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse open hours for vendor {}: {}", vendorId, e.getMessage());
                // If we can't parse, allow the order
            }
        }

        return true;
    }

    /**
     * Process scheduled orders that are due within the processing window.
     * Runs every 60 seconds. Finds orders with scheduledTime &lt;= now + 15 min
     * and status = PLACED, then transitions them to ACCEPTED.
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledOrders() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(PROCESSING_WINDOW_MINUTES);

        List<Order> dueOrders = orderRepository.findAll().stream()
                .filter(o -> o.getScheduledTime() != null)
                .filter(o -> o.getStatus() == OrderStatus.PLACED)
                .filter(o -> !o.getScheduledTime().isAfter(cutoff))
                .collect(Collectors.toList());

        if (dueOrders.isEmpty()) return;

        log.info("Processing {} scheduled orders due by {}", dueOrders.size(), cutoff);

        for (Order order : dueOrders) {
            try {
                order.setStatus(OrderStatus.ACCEPTED);
                orderRepository.save(order);
                log.info("Scheduled order {} transitioned to ACCEPTED", order.getId());
            } catch (Exception e) {
                log.error("Failed to process scheduled order {}: {}", order.getId(), e.getMessage());
            }
        }
    }

    /**
     * Get upcoming scheduled orders for a vendor.
     *
     * @param vendorId the vendor UUID
     * @return list of order DTOs for scheduled orders that are PLACED or ACCEPTED
     */
    @Transactional(readOnly = true)
    public List<OrderResponseDTO> getScheduledOrders(UUID vendorId) {
        List<Order> orders = orderRepository.findByVendorIdAndStatus(vendorId, OrderStatus.PLACED);
        List<Order> acceptedOrders = orderRepository.findByVendorIdAndStatus(vendorId, OrderStatus.ACCEPTED);

        List<Order> allScheduled = new java.util.ArrayList<>();
        allScheduled.addAll(orders);
        allScheduled.addAll(acceptedOrders);

        return allScheduled.stream()
                .filter(o -> o.getScheduledTime() != null)
                .sorted((a, b) -> a.getScheduledTime().compareTo(b.getScheduledTime()))
                .map(orderMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Build a validation result map.
     */
    public Map<String, Object> buildValidationResult(boolean valid, String message) {
        return Map.of("valid", valid, "message", message);
    }
}
