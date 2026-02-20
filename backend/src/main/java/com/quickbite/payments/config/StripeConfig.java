package com.quickbite.payments.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe payment gateway configuration.
 * Sets the global Stripe API key from application properties.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class StripeConfig {

    private final PaymentProperties paymentProperties;

    @PostConstruct
    public void init() {
        String apiKey = paymentProperties.getApiKey();
        if (apiKey != null && (apiKey.startsWith("sk_test_") || apiKey.startsWith("sk_live_"))) {
            Stripe.apiKey = apiKey;
            String mode = apiKey.startsWith("sk_test_") ? "TEST" : "LIVE";
            log.info("Stripe initialized in {} mode", mode);
        } else {
            log.warn("Stripe API key not configured or invalid — payments will use stub mode. " +
                     "Set payments.api-key in application properties.");
        }
    }
}
