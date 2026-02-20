package com.quickbite.payments.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for payment provider integration.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "payments")
public class PaymentProperties {

    private String provider = "generic"; // generic, razorpay, stripe
    
    private String apiKey;

    /** Stripe Publishable Key (pk_test_... or pk_live_...) — sent to frontend. */
    private String publishableKey;
    
    private String apiSecret;
    
    private Webhook webhook = new Webhook();
    
    @Data
    public static class Webhook {
        private String secret = "change-me";
        private String headerName = "X-Signature";
    }
}
