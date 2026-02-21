package com.quickbite.notifications.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.notifications.entity.Notification;
import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.repository.NotificationRepository;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController.
 * Phase 4 — Testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private User customer;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        customer = userRepository.save(User.builder()
                .email("notif-cust-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Notif Customer")
                .role(customerRole)
                .active(true)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");

        // Seed some notifications
        for (int i = 1; i <= 3; i++) {
            Notification n = Notification.builder()
                    .user(customer)
                    .type(NotificationType.ORDER_UPDATE)
                    .title("Notification " + i)
                    .message("Your order #" + i + " was updated")
                    .isRead(i == 1)  // First one is read
                    .build();
            notificationRepository.save(n);
        }
    }

    @Test
    void listNotifications_returnsAll() throws Exception {
        mockMvc.perform(get("/api/notifications?page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void getUnreadCount_returnsCorrectCount() throws Exception {
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(2));  // 3 total, 1 read = 2 unread
    }

    @Test
    void markAllRead_thenUnreadCountZero() throws Exception {
        // Mark all as read
        mockMvc.perform(post("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // Unread count should be 0
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(0));
    }
}
