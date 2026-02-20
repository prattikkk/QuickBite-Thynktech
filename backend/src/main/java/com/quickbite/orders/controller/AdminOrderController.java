package com.quickbite.orders.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.orders.entity.EventTimeline;
import com.quickbite.orders.service.EventTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-only endpoints for order support and auditing.
 */
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final EventTimelineService eventTimelineService;

    /**
     * Get the full audit timeline for an order.
     * Includes every status change, payment event, and assignment.
     */
    @GetMapping("/{orderId}/timeline")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOrderTimeline(
            @PathVariable UUID orderId) {

        List<EventTimeline> entries = eventTimelineService.getTimeline(orderId);

        // Map to a clean response shape
        List<Map<String, Object>> timeline = entries.stream()
                .map(e -> {
                    Map<String, Object> item = new java.util.LinkedHashMap<>();
                    item.put("id", e.getId());
                    item.put("eventType", e.getEventType());
                    item.put("oldStatus", e.getOldStatus());
                    item.put("newStatus", e.getNewStatus());
                    item.put("actorId", e.getActorId());
                    item.put("actorRole", e.getActorRole());
                    item.put("meta", e.getMeta());
                    item.put("createdAt", e.getCreatedAt());
                    return item;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.success(timeline));
    }
}
