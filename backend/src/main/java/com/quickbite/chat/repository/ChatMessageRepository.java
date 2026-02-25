package com.quickbite.chat.repository;

import com.quickbite.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository interface for ChatMessage entity operations.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    /**
     * Find messages for a room, ordered by newest first (paginated).
     */
    Page<ChatMessage> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

    /**
     * Count unread messages in a room that were NOT sent by the given user.
     */
    long countByRoomIdAndReadFalseAndSenderIdNot(UUID roomId, UUID userId);

    /**
     * Mark all messages in a room as read where the sender is not the given user.
     */
    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true WHERE m.room.id = :roomId AND m.sender.id <> :userId AND m.read = false")
    int markAllAsRead(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}
