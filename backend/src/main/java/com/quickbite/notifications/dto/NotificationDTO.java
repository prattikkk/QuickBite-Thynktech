package com.quickbite.notifications.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for notification response.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NotificationDTO {
    private UUID id;
    private String type;
    private String title;
    private String message;
    private UUID refId;
    private Boolean isRead;
    private OffsetDateTime createdAt;
}
