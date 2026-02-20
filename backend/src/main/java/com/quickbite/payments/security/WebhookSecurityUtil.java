package com.quickbite.payments.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for webhook security operations.
 * Provides HMAC-SHA256 signature verification for webhook payloads.
 */
@Slf4j
@UtilityClass
public class WebhookSecurityUtil {

    /**
     * Verify HMAC-SHA256 signature for webhook payload.
     *
     * @param payload raw webhook payload
     * @param signatureHeader signature from provider
     * @param secret webhook secret
     * @return true if signature is valid
     */
    public static boolean verifySignatureHmacSha256(String payload, String signatureHeader, String secret) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Webhook signature header is missing");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            // Try hex encoding first (common for Razorpay, custom implementations)
            String expectedHex = bytesToHex(digest);
            if (expectedHex.equalsIgnoreCase(signatureHeader)) {
                return true;
            }

            // Try base64 encoding (common for Stripe)
            String expectedBase64 = Base64.getEncoder().encodeToString(digest);
            if (expectedBase64.equals(signatureHeader)) {
                return true;
            }

            log.warn("Webhook signature mismatch. Expected (hex): {}, Expected (base64): {}, Received: {}",
                    expectedHex, expectedBase64, signatureHeader);
            return false;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    /**
     * Convert byte array to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verify Razorpay-specific signature format.
     * Razorpay uses: hmac_sha256(webhook_secret, webhook_body)
     */
    public static boolean verifyRazorpaySignature(String payload, String signature, String secret) {
        return verifySignatureHmacSha256(payload, signature, secret);
    }

    /**
     * Verify Stripe-specific signature format.
     * Stripe uses: v1=timestamp.signature format
     */
    public static boolean verifyStripeSignature(String payload, String signatureHeader, String secret) {
        // Stripe format: t=timestamp,v1=signature
        if (signatureHeader == null || !signatureHeader.contains("v1=")) {
            return false;
        }

        String[] parts = signatureHeader.split(",");
        String timestamp = null;
        String signature = null;

        for (String part : parts) {
            if (part.startsWith("t=")) {
                timestamp = part.substring(2);
            } else if (part.startsWith("v1=")) {
                signature = part.substring(3);
            }
        }

        if (timestamp == null || signature == null) {
            return false;
        }

        // Stripe signature is: hmac_sha256(secret, timestamp.payload)
        String signedPayload = timestamp + "." + payload;
        return verifySignatureHmacSha256(signedPayload, signature, secret);
    }
}
