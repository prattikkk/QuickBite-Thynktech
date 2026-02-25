package com.quickbite.push.service;

import com.quickbite.push.entity.DeviceToken;
import com.quickbite.push.repository.DeviceTokenRepository;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Push notification service.
 * In dev mode (push.provider=console), logs push payloads.
 * In production, would integrate with FCM Admin SDK.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Value("${push.enabled:false}")
    private boolean pushEnabled;

    @Value("${push.provider:console}")
    private String pushProvider;

    /**
     * Register a device token for push notifications.
     */
    @Transactional
    public void registerDevice(UUID userId, String token, String platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Upsert - if token already exists for this user, just update
        deviceTokenRepository.findByUserIdAndToken(userId, token).ifPresentOrElse(
                existing -> {
                    existing.setPlatform(platform != null ? platform : "WEB");
                    deviceTokenRepository.save(existing);
                    log.debug("Updated device token for user {}", userId);
                },
                () -> {
                    DeviceToken dt = DeviceToken.builder()
                            .user(user)
                            .token(token)
                            .platform(platform != null ? platform : "WEB")
                            .build();
                    deviceTokenRepository.save(dt);
                    log.info("Registered device token for user {} platform={}", userId, platform);
                }
        );
    }

    /**
     * Unregister a device token.
     */
    @Transactional
    public void unregisterDevice(UUID userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
        log.info("Unregistered device token for user {}", userId);
    }

    /**
     * Send push notification to a user (all their devices).
     */
    @Async
    public void sendPushToUser(UUID userId, String title, String body, String refId) {
        if (!pushEnabled) return;

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens for user {}, skipping push", userId);
            return;
        }

        for (DeviceToken dt : tokens) {
            sendPush(dt.getToken(), dt.getPlatform(), title, body, refId);
        }
    }

    private void sendPush(String token, String platform, String title, String body, String refId) {
        if ("console".equals(pushProvider)) {
            log.info("========== CONSOLE PUSH ==========");
            log.info("Token:    {}...{}", token.substring(0, Math.min(8, token.length())), token.length() > 8 ? "..." : "");
            log.info("Platform: {}", platform);
            log.info("Title:    {}", title);
            log.info("Body:     {}", body);
            log.info("RefId:    {}", refId);
            log.info("==================================");
        } else {
            // TODO: Integrate with FCM Admin SDK for production
            // FirebaseMessaging.getInstance().send(Message.builder()
            //     .setToken(token)
            //     .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            //     .putData("refId", refId)
            //     .build());
            log.info("Push sent via {} to token={} title={}", pushProvider, token.substring(0, 8), title);
        }
    }
}
