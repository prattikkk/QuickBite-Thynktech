package com.quickbite.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for chat room responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {

    private UUID id;
    private UUID orderId;
    private UUID otherUserId;
    private String otherUserName;
    private String roomType;
    private boolean closed;
    private String lastMessage;
    private OffsetDateTime lastMessageAt;
    private long unreadCount;
    private OffsetDateTime createdAt;
}
