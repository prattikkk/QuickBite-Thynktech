package com.quickbite.orders.repository;

import com.quickbite.orders.entity.EventTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventTimelineRepository extends JpaRepository<EventTimeline, UUID> {

    List<EventTimeline> findByOrderIdOrderByCreatedAtAsc(UUID orderId);

    long countByOrderId(UUID orderId);
}
