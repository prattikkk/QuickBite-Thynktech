package com.quickbite.chat.repository;

import com.quickbite.chat.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ChatRoom entity operations.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    /**
     * Find a chat room by order ID and room type.
     */
    Optional<ChatRoom> findByOrderIdAndRoomType(UUID orderId, String roomType);

    /**
     * Find all chat rooms where the user is either participant.
     */
    List<ChatRoom> findByParticipant1IdOrParticipant2Id(UUID participant1Id, UUID participant2Id);

    /**
     * Find all chat rooms associated with a given order.
     */
    List<ChatRoom> findByOrderId(UUID orderId);
}
