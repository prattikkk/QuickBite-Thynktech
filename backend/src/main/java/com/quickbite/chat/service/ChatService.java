package com.quickbite.chat.service;

import com.quickbite.chat.dto.ChatMessageDTO;
import com.quickbite.chat.dto.ChatRoomDTO;
import com.quickbite.chat.entity.ChatMessage;
import com.quickbite.chat.entity.ChatRoom;
import com.quickbite.chat.repository.ChatMessageRepository;
import com.quickbite.chat.repository.ChatRoomRepository;
import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.service.NotificationService;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for chat room and message operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;

    /**
     * Get an existing chat room or create a new one for the given order and room type.
     */
    @Transactional
    public ChatRoom getOrCreateRoom(UUID orderId, UUID userId, UUID otherUserId, String roomType) {
        return chatRoomRepository.findByOrderIdAndRoomType(orderId, roomType)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    User otherUser = userRepository.findById(otherUserId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + otherUserId));

                    ChatRoom room = ChatRoom.builder()
                            .orderId(orderId)
                            .participant1(user)
                            .participant2(otherUser)
                            .roomType(roomType)
                            .build();

                    log.info("Creating chat room for order {} type {} between {} and {}", orderId, roomType, userId, otherUserId);
                    return chatRoomRepository.save(room);
                });
    }

    /**
     * Send a message in a chat room and broadcast via STOMP.
     */
    @Transactional
    public ChatMessage sendMessage(UUID roomId, UUID senderId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        if (Boolean.TRUE.equals(room.getClosed())) {
            throw new IllegalStateException("Chat room is closed");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + senderId));

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(content)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);

        // Broadcast to WebSocket subscribers
        ChatMessageDTO dto = toMessageDTO(saved);
        messagingTemplate.convertAndSend("/topic/chat." + roomId, dto);
        log.debug("Message sent in room {} by user {}", roomId, senderId);

        // Send notification to the recipient
        UUID recipientId = room.getParticipant1().getId().equals(senderId)
                ? room.getParticipant2().getId()
                : room.getParticipant1().getId();
        
        try {
            notificationService.createNotification(
                recipientId,
                NotificationType.CHAT_MESSAGE,
                "New message from " + sender.getName(),
                content.length() > 50 ? content.substring(0, 50) + "..." : content,
                room.getOrderId()
            );
            log.debug("Notification sent to user {} for chat message", recipientId);
        } catch (Exception e) {
            log.warn("Failed to send notification for chat message: {}", e.getMessage());
        }

        return saved;
    }

    /**
     * Get paginated messages for a chat room.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageDTO> getMessages(UUID roomId, UUID userId, int page, int size) {
        // Verify room exists
        chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        Page<ChatMessage> messages = chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size));

        return messages.map(this::toMessageDTO);
    }

    /**
     * Mark all unread messages in a room as read for the given user.
     */
    @Transactional
    public void markRead(UUID roomId, UUID userId) {
        int updated = chatMessageRepository.markAllAsRead(roomId, userId);
        log.debug("Marked {} messages as read in room {} for user {}", updated, roomId, userId);
    }

    /**
     * Get all chat rooms for a user.
     */
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getUserRooms(UUID userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipant1IdOrParticipant2Id(userId, userId);

        return rooms.stream()
                .map(room -> toRoomDTO(room, userId))
                .collect(Collectors.toList());
    }

    // ========== Mapping Helpers ==========

    private ChatMessageDTO toMessageDTO(ChatMessage message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .roomId(message.getRoom().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .content(message.getContent())
                .read(Boolean.TRUE.equals(message.getRead()))
                .createdAt(message.getCreatedAt())
                .build();
    }

    private ChatRoomDTO toRoomDTO(ChatRoom room, UUID userId) {
        User otherUser = room.getParticipant1().getId().equals(userId)
                ? room.getParticipant2()
                : room.getParticipant1();

        // Fetch last message for preview
        Page<ChatMessage> lastMsgPage = chatMessageRepository
                .findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, 1));

        String lastMessage = null;
        java.time.OffsetDateTime lastMessageAt = null;
        if (lastMsgPage.hasContent()) {
            ChatMessage lastMsg = lastMsgPage.getContent().get(0);
            lastMessage = lastMsg.getContent();
            lastMessageAt = lastMsg.getCreatedAt();
        }

        long unreadCount = chatMessageRepository
                .countByRoomIdAndReadFalseAndSenderIdNot(room.getId(), userId);

        return ChatRoomDTO.builder()
                .id(room.getId())
                .orderId(room.getOrderId())
                .otherUserId(otherUser.getId())
                .otherUserName(otherUser.getName())
                .roomType(room.getRoomType())
                .closed(Boolean.TRUE.equals(room.getClosed()))
                .lastMessage(lastMessage)
                .lastMessageAt(lastMessageAt)
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }
}
