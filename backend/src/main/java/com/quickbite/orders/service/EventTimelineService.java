package com.quickbite.orders.service;

import com.quickbite.orders.entity.EventTimeline;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.EventTimelineRepository;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Records auditable timeline entries for every significant order event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventTimelineService {

    private final EventTimelineRepository repository;
    private final UserRepository userRepository;

    /**
     * Record a status change event.
     */
    @Transactional
    public EventTimeline recordStatusChange(UUID orderId, UUID actorId,
                                             OrderStatus oldStatus, OrderStatus newStatus,
                                             Map<String, Object> meta) {
        String actorRole = resolveRole(actorId);

        EventTimeline entry = EventTimeline.builder()
                .orderId(orderId)
                .actorId(actorId)
                .actorRole(actorRole)
                .eventType("STATUS_CHANGE")
                .oldStatus(oldStatus != null ? oldStatus.name() : null)
                .newStatus(newStatus != null ? newStatus.name() : null)
                .meta(meta)
                .build();

        entry = repository.save(entry);
        log.debug("Timeline entry: order={} {} → {} by {} ({})",
                orderId, oldStatus, newStatus, actorId, actorRole);
        return entry;
    }

    /**
     * Record a generic event (payment, assignment, etc.).
     */
    @Transactional
    public EventTimeline recordEvent(UUID orderId, UUID actorId,
                                      String eventType, Map<String, Object> meta) {
        String actorRole = resolveRole(actorId);

        EventTimeline entry = EventTimeline.builder()
                .orderId(orderId)
                .actorId(actorId)
                .actorRole(actorRole)
                .eventType(eventType)
                .meta(meta)
                .build();

        return repository.save(entry);
    }

    /**
     * Get the full timeline for an order.
     */
    @Transactional(readOnly = true)
    public List<EventTimeline> getTimeline(UUID orderId) {
        return repository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    private String resolveRole(UUID userId) {
        if (userId == null) return "SYSTEM";
        return userRepository.findById(userId)
                .map(u -> u.getRole().getName())
                .orElse("UNKNOWN");
    }
}
