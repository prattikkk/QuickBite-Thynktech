package com.quickbite.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * High-level SMS dispatch — decides message content and sends asynchronously.
 * Guarded by feature flag: sms.enabled
 * Rate-limited: max 5 SMS per phone number per hour (PRD 3.3).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    private final SmsService smsService;
    private final StringRedisTemplate redisTemplate;

    private static final int MAX_SMS_PER_HOUR = 5;
    private static final String RATE_KEY_PREFIX = "sms:rate:";

    @Value("${sms.enabled:true}")
    private boolean smsEnabled;

    /**
     * Sent immediately after registration when the user provided a phone number.
     * Contains the email verification link so they can verify from mobile too.
     */
    @Async
    public void sendRegistrationSms(String phone, String userName, String verifyLink) {
        if (!canSend(phone)) return;
        String msg = "Hi " + userName + "! Welcome to QuickBite. "
                + "Please verify your email to start ordering: " + verifyLink;
        smsService.sendSms(phone, msg);
    }

    @Async
    public void sendOrderPlacedSms(String phone, String orderNumber) {
        if (!canSend(phone)) return;
        smsService.sendSms(phone, "QuickBite: Your order #" + orderNumber + " has been placed!");
    }

    @Async
    public void sendOrderDeliveredSms(String phone, String orderNumber) {
        if (!canSend(phone)) return;
        smsService.sendSms(phone, "QuickBite: Your order #" + orderNumber + " has been delivered. Enjoy!");
    }

    @Async
    public void sendOutForDeliverySms(String phone, String orderNumber) {
        if (!canSend(phone)) return;
        smsService.sendSms(phone, "QuickBite: Your order #" + orderNumber + " is on the way!");
    }

    @Async
    public void sendOtpSms(String phone, String otp) {
        if (!canSend(phone)) return;
        smsService.sendSms(phone, "QuickBite: Your verification code is " + otp + ". Valid for 10 minutes.");
    }

    /**
     * Check feature flag, null/blank phone, and per-phone rate limit.
     * Increments the counter on success.
     */
    private boolean canSend(String phone) {
        if (!smsEnabled || phone == null || phone.isBlank()) return false;
        try {
            String key = RATE_KEY_PREFIX + phone.replaceAll("\\D", "");
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // First SMS this window — set expiry to 1 hour
                redisTemplate.expire(key, Duration.ofHours(1));
            }
            if (count != null && count > MAX_SMS_PER_HOUR) {
                log.warn("SMS rate limit exceeded for phone={}, count={}", phone, count);
                return false;
            }
        } catch (Exception e) {
            log.warn("SMS rate limiter unavailable, allowing send: {}", e.getMessage());
        }
        return true;
    }
}
