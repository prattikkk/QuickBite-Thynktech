package com.quickbite.notifications.service;

import com.quickbite.notifications.dto.NotificationDTO;
import com.quickbite.notifications.entity.Notification;
import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.repository.NotificationRepository;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for creating and managing in-app notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Create a notification for a user.
     */
    @Transactional
    public NotificationDTO createNotification(UUID userId, NotificationType type,
                                               String title, String message, UUID refId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));

        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .refId(refId)
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification created for user {}: {}", userId, title);
        return toDTO(notification);
    }

    /**
     * Get paginated notifications for a user.
     */
    @Transactional(readOnly = true)
    public Page<NotificationDTO> getNotifications(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get unread notification count.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Mark a notification as read.
     */
    @Transactional
    public NotificationDTO markAsRead(UUID notificationId, UUID userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException("Notification not found"));

        if (!n.getUser().getId().equals(userId)) {
            throw new BusinessException("Access denied to notification");
        }

        n.setIsRead(true);
        n = notificationRepository.save(n);
        return toDTO(n);
    }

    /**
     * Mark all notifications as read for a user.
     */
    @Transactional
    public void markAllAsRead(UUID userId) {
        Pageable all = PageRequest.of(0, 1000);
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, all);
        notifications.forEach(n -> {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
        log.info("All notifications marked as read for user {}", userId);
    }

    private NotificationDTO toDTO(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .refId(n.getRefId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
