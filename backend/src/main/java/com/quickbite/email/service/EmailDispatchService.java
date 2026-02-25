package com.quickbite.email.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * High-level email dispatch — decides which template to use and sends asynchronously.
 * Guarded by feature flag: email.enabled
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDispatchService {

    private final EmailService emailService;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    // -------- Public dispatch methods (all @Async) --------

    @Async
    public void sendWelcomeEmail(String to, String userName) {
        if (!emailEnabled) return;
        emailService.sendTemplatedEmail(to, "Welcome to QuickBite!",
                "welcome", Map.of("userName", userName, "baseUrl", baseUrl));
    }

    @Async
    public void sendPasswordResetEmail(String to, String userName, String token) {
        if (!emailEnabled) return;
        String resetLink = baseUrl + "/reset-password?token=" + token;
        emailService.sendTemplatedEmail(to, "Reset your QuickBite password",
                "password-reset", Map.of("userName", userName, "resetLink", resetLink, "baseUrl", baseUrl));
    }

    @Async
    public void sendEmailVerification(String to, String userName, String token) {
        if (!emailEnabled) return;
        String verifyLink = buildVerifyLink(token);
        emailService.sendTemplatedEmail(to, "Verify your QuickBite email",
                "email-verification", Map.of("userName", userName, "verifyLink", verifyLink, "baseUrl", baseUrl));
    }

    /**
     * Build the email-verification URL for a given raw token.
     * Usable by other services (e.g. SmsDispatchService) without going through the full email flow.
     */
    public String buildVerifyLink(String token) {
        return baseUrl + "/verify-email?token=" + token;
    }

    @Async
    public void sendOrderConfirmation(String to, String userName, String orderNumber,
                                       long totalCents, String vendorName) {
        if (!emailEnabled) return;
        String total = String.format("$%.2f", totalCents / 100.0);
        emailService.sendTemplatedEmail(to, "Order Confirmed — #" + orderNumber,
                "order-confirmation",
                Map.of("userName", userName, "orderNumber", orderNumber,
                        "total", total, "vendorName", vendorName, "baseUrl", baseUrl));
    }

    @Async
    public void sendOrderStatusUpdate(String to, String userName, String orderNumber,
                                       String oldStatus, String newStatus) {
        if (!emailEnabled) return;
        emailService.sendTemplatedEmail(to, "Order #" + orderNumber + " — " + newStatus,
                "order-status",
                Map.of("userName", userName, "orderNumber", orderNumber,
                        "oldStatus", oldStatus, "newStatus", newStatus, "baseUrl", baseUrl));
    }
}
