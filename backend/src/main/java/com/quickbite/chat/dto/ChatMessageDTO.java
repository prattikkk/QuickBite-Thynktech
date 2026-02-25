package com.quickbite.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for chat message responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {

    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String content;
    private boolean read;
    private OffsetDateTime createdAt;
}
