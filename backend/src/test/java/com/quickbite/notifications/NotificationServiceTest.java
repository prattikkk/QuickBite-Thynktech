package com.quickbite.notifications;

import com.quickbite.notifications.dto.NotificationDTO;
import com.quickbite.notifications.entity.Notification;
import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.repository.NotificationRepository;
import com.quickbite.notifications.service.NotificationService;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService — Phase 3 notifications feature.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;

    private NotificationService notificationService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, userRepository);

        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("customer@test.com")
                .name("Test Customer")
                .build();
    }

    @Test
    @DisplayName("createNotification — saves and returns DTO")
    void createNotification_success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(UUID.randomUUID());
            n.setCreatedAt(OffsetDateTime.now());
            return n;
        });

        UUID refId = UUID.randomUUID();
        NotificationDTO dto = notificationService.createNotification(
                userId, NotificationType.ORDER_UPDATE, "Order Placed",
                "Your order has been placed!", refId);

        assertThat(dto).isNotNull();
        assertThat(dto.getTitle()).isEqualTo("Order Placed");
        assertThat(dto.getType()).isEqualTo("ORDER_UPDATE");
        assertThat(dto.getIsRead()).isFalse();
        assertThat(dto.getRefId()).isEqualTo(refId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("getNotifications — returns paginated list")
    void getNotifications_success() {
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .user(user)
                .type(NotificationType.ORDER_UPDATE)
                .title("Order Placed")
                .message("Your order has been placed!")
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(page);

        Page<NotificationDTO> result = notificationService.getNotifications(userId, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Order Placed");
    }

    @Test
    @DisplayName("getUnreadCount — returns count")
    void getUnreadCount_success() {
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(userId);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("markAsRead — marks notification as read")
    void markAsRead_success() {
        UUID notifId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .user(user)
                .type(NotificationType.ORDER_UPDATE)
                .title("Test")
                .message("Test message")
                .isRead(false)
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenReturn(notification);

        notificationService.markAsRead(notifId, userId);

        assertThat(notification.getIsRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("markAsRead — wrong user throws exception")
    void markAsRead_wrongUser_throws() {
        UUID notifId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notifId)
                .user(user)
                .type(NotificationType.ORDER_UPDATE)
                .title("Test")
                .message("Test message")
                .isRead(false)
                .build();

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notification));

        UUID otherUser = UUID.randomUUID();
        assertThatThrownBy(() -> notificationService.markAsRead(notifId, otherUser))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("markAllAsRead — updates all unread for user")
    void markAllAsRead_success() {
        Notification n1 = Notification.builder().id(UUID.randomUUID()).user(user).isRead(false).build();
        Notification n2 = Notification.builder().id(UUID.randomUUID()).user(user).isRead(false).build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(n1, n2)));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markAllAsRead(userId);

        verify(notificationRepository, times(2)).save(any(Notification.class));
        assertThat(n1.getIsRead()).isTrue();
        assertThat(n2.getIsRead()).isTrue();
    }
}
