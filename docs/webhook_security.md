# Webhook Security

## Overview

Payment provider webhooks deliver critical payment status updates. Proper security is essential to prevent:
- **Replay attacks** - Malicious actors resending legitimate webhooks
- **Spoofing** - Fake webhooks from unauthorized sources
- **Data tampering** - Modified webhook payloads

QuickBite implements **HMAC-SHA256 signature verification** and **idempotency checks** to ensure webhook integrity.

## Architecture

```
┌──────────────────┐
│ Payment Provider │
│ (Razorpay/Stripe)│
└────────┬─────────┘
         │
         │ 1. Generate HMAC signature
         │    using webhook secret
         │
         ▼
┌─────────────────────────────────┐
│ POST /api/payments/webhook       │
│ X-Razorpay-Signature: abc123...  │
│ Body: {"event":"payment.captured"}│
└────────┬────────────────────────┘
         │
         │ 2. Verify signature
         ▼
┌─────────────────────┐
│ WebhookSecurityUtil │
│ - verifySignature() │
└────────┬────────────┘
         │
         │ 3. Check idempotency
         ▼
┌──────────────────────┐
│ WebhookEventRepo     │
│ - Check event_id     │
└────────┬─────────────┘
         │
         │ 4. Process event
         ▼
┌──────────────────────┐
│ PaymentService       │
│ - Update payment     │
│ - Update order       │
└──────────────────────┘
```

## HMAC Signature Verification

### Razorpay Signature

**Algorithm:** HMAC-SHA256 (hex-encoded)

**Input:** `{webhook_body}` (raw string)

**Process:**
```java
String expectedSignature = WebhookSecurityUtil.verifyRazorpaySignature(
    rawBody,
    receivedSignature,
    webhookSecret
);
```

**Implementation:**
```java
public static boolean verifyRazorpaySignature(String payload, String signature, String secret) {
    String computed = computeHmacSha256Hex(payload, secret);
    return MessageDigest.isEqual(computed.getBytes(), signature.getBytes());
}

private static String computeHmacSha256Hex(String data, String secret) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
    mac.init(secretKey);
    byte[] hmac = mac.doFinal(data.getBytes(UTF_8));
    return bytesToHex(hmac); // Convert to hex string
}
```

**Header:** `X-Razorpay-Signature: {hex_signature}`

### Stripe Signature

**Algorithm:** HMAC-SHA256 (base64-encoded with timestamp)

**Format:** `t={timestamp},v1={signature}`

**Input:** `{timestamp}.{webhook_body}`

**Process:**
```java
boolean valid = WebhookSecurityUtil.verifyStripeSignature(
    rawBody,
    "t=1234567890,v1=abc123def456",
    webhookSecret
);
```

**Implementation:**
```java
public static boolean verifyStripeSignature(String payload, String signatureHeader, String secret) {
    // Parse: t=1234567890,v1=signature
    String[] parts = signatureHeader.split(",");
    String timestamp = null;
    String signature = null;
    
    for (String part : parts) {
        String[] kv = part.split("=", 2);
        if (kv[0].equals("t")) timestamp = kv[1];
        if (kv[0].equals("v1")) signature = kv[1];
    }
    
    // Compute: {timestamp}.{payload}
    String signedPayload = timestamp + "." + payload;
    String computed = computeHmacSha256Base64(signedPayload, secret);
    
    return MessageDigest.isEqual(computed.getBytes(), signature.getBytes());
}
```

**Header:** `Stripe-Signature: t=1234567890,v1={base64_signature}`

### Generic HMAC Verification

For other providers or custom implementation:

```java
boolean valid = WebhookSecurityUtil.verifySignatureHmacSha256(
    rawBody,
    receivedSignature,
    webhookSecret
);
```

Supports both **hex** and **base64** encoding (auto-detected).

## Idempotency

### Why Idempotency?

Payment providers retry webhook delivery on failure (no 2xx response). Without idempotency:
- Payment captured twice
- Order status updated multiple times
- Notifications sent repeatedly

### Implementation

**Database Table:**
```sql
CREATE TABLE webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    processed BOOLEAN DEFAULT FALSE,
    processing_error TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    processed_at TIMESTAMPTZ
);

CREATE INDEX idx_webhook_events_provider_event_id ON webhook_events(provider_event_id);
CREATE INDEX idx_webhook_events_processed_created ON webhook_events(processed, created_at);
```

**Idempotency Check:**
```java
@Transactional
public boolean handleWebhook(String rawBody, String signature) {
    // 1. Verify signature
    if (!verifyWebhookSignature(rawBody, signature)) {
        return false;
    }
    
    // 2. Extract event ID
    JsonNode root = objectMapper.readTree(rawBody);
    String providerEventId = extractEventId(root);
    
    // 3. Check if already processed
    if (webhookEventRepository.existsByProviderEventId(providerEventId)) {
        log.info("Webhook {} already processed (idempotent)", providerEventId);
        return true; // Return success to prevent retry
    }
    
    // 4. Store event
    WebhookEvent event = WebhookEvent.builder()
        .providerEventId(providerEventId)
        .eventType(extractEventType(root))
        .payload(rawBody)
        .processed(false)
        .build();
    webhookEventRepository.save(event);
    
    // 5. Process event
    boolean success = processWebhookEvent(root);
    
    // 6. Mark as processed
    event.setProcessed(success);
    event.setProcessedAt(OffsetDateTime.now());
    webhookEventRepository.save(event);
    
    return success;
}
```

## Security Best Practices

### 1. Webhook Secret Management

**❌ Don't:**
```java
String secret = "my-webhook-secret"; // Hardcoded
```

**✅ Do:**
```bash
# Environment variable
PAYMENT_WEBHOOK_SECRET=whsec_complex_random_string_256_bits

# Or secrets manager (AWS Secrets Manager, HashiCorp Vault)
```

```java
@Value("${payments.webhook.secret}")
private String webhookSecret;
```

### 2. Constant-Time Comparison

**❌ Don't:**
```java
if (computedSignature.equals(receivedSignature)) { // Timing attack vulnerable
    // ...
}
```

**✅ Do:**
```java
if (MessageDigest.isEqual(
    computedSignature.getBytes(),
    receivedSignature.getBytes()
)) {
    // Constant-time comparison
}
```

### 3. Raw Body Preservation

**❌ Don't:**
```java
@PostMapping("/webhook")
public void webhook(@RequestBody WebhookDTO dto) {
    // Body already parsed - signature verification impossible
}
```

**✅ Do:**
```java
@PostMapping("/webhook")
public ResponseEntity<?> webhook(
    @RequestBody String rawBody,
    @RequestHeader("X-Razorpay-Signature") String signature
) {
    // Verify signature on raw body
    if (!verifySignature(rawBody, signature)) {
        return ResponseEntity.status(401).build();
    }
    // Then parse body
}
```

### 4. Timestamp Validation (Stripe)

Prevent replay attacks with old signatures:

```java
public static boolean verifyStripeSignature(
    String payload,
    String signatureHeader,
    String secret,
    long toleranceSeconds // e.g., 300 = 5 minutes
) {
    long timestamp = extractTimestamp(signatureHeader);
    long currentTime = System.currentTimeMillis() / 1000;
    
    if (Math.abs(currentTime - timestamp) > toleranceSeconds) {
        log.warn("Webhook timestamp too old: {}", timestamp);
        return false; // Reject old signatures
    }
    
    // Continue with signature verification...
}
```

### 5. IP Whitelisting (Optional)

Some providers publish webhook IP ranges:

```java
private static final Set<String> RAZORPAY_IPS = Set.of(
    "3.6.128.0/20",
    "3.7.4.0/22"
    // ... (from Razorpay docs)
);

@PostMapping("/webhook")
public ResponseEntity<?> webhook(HttpServletRequest request) {
    String clientIp = request.getRemoteAddr();
    if (!isWhitelisted(clientIp)) {
        log.warn("Webhook from unauthorized IP: {}", clientIp);
        return ResponseEntity.status(403).build();
    }
    // Continue...
}
```

### 6. Rate Limiting

Prevent webhook flooding:

```java
@RateLimiter(name = "webhook", fallbackMethod = "webhookFallback")
@PostMapping("/webhook")
public ResponseEntity<?> webhook(@RequestBody String rawBody) {
    // Process webhook
}

public ResponseEntity<?> webhookFallback(Exception e) {
    return ResponseEntity.status(429).body("Too many requests");
}
```

### 7. No JWT Authentication

**❌ Don't:**
```java
@PreAuthorize("isAuthenticated()") // Webhooks have no JWT!
@PostMapping("/webhook")
public void webhook() { }
```

**✅ Do:**
```java
@PostMapping("/webhook") // No @PreAuthorize
public ResponseEntity<?> webhook() {
    // HMAC signature verification replaces JWT
}
```

Configure Spring Security to allow webhook endpoint:

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/payments/webhook").permitAll() // Allow webhooks
            .anyRequest().authenticated()
        )
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/payments/webhook") // Disable CSRF for webhooks
        );
    return http.build();
}
```

## Monitoring & Alerting

### 1. Log Failed Verifications

```java
if (!verifyWebhookSignature(rawBody, signature)) {
    log.error("Webhook signature verification failed. IP: {}, Body: {}",
        request.getRemoteAddr(), rawBody);
    // Alert security team
    return ResponseEntity.status(401).build();
}
```

### 2. Track Processing Failures

```sql
-- Query failed webhooks
SELECT * FROM webhook_events
WHERE processed = FALSE
AND created_at > NOW() - INTERVAL '1 hour'
ORDER BY created_at DESC;
```

### 3. Duplicate Event Metrics

```java
if (webhookEventRepository.existsByProviderEventId(eventId)) {
    metricsService.incrementCounter("webhook.duplicate");
    return true;
}
```

## Testing

### 1. Mock Webhook with Valid Signature

```java
@Test
void testWebhookWithValidSignature() {
    String payload = """
        {
            "event": "payment.captured",
            "payload": {
                "payment": {"entity": {"id": "pay_123"}}
            }
        }
        """;
    
    String secret = "test-secret";
    String signature = WebhookSecurityUtil.computeHmacSha256Hex(payload, secret);
    
    mockMvc.perform(post("/api/payments/webhook")
            .content(payload)
            .header("X-Razorpay-Signature", signature))
        .andExpect(status().isOk());
}
```

### 2. Test Invalid Signature

```java
@Test
void testWebhookWithInvalidSignature() {
    mockMvc.perform(post("/api/payments/webhook")
            .content("{}")
            .header("X-Razorpay-Signature", "invalid"))
        .andExpect(status().isUnauthorized());
}
```

### 3. Test Idempotency

```java
@Test
void testWebhookIdempotency() {
    String payload = "...";
    String signature = "...";
    
    // First call - should process
    mockMvc.perform(post("/api/payments/webhook")
            .content(payload)
            .header("X-Razorpay-Signature", signature))
        .andExpect(status().isOk());
    
    // Second call - should be idempotent
    mockMvc.perform(post("/api/payments/webhook")
            .content(payload)
            .header("X-Razorpay-Signature", signature))
        .andExpect(status().isOk());
    
    // Verify payment only updated once
    assertEquals(1, paymentRepository.count());
}
```

## Debugging Checklist

❓ **Signature verification failing?**
- [ ] Check webhook secret matches provider dashboard
- [ ] Verify raw body not modified (JSON formatting, encoding)
- [ ] Check signature header name (X-Razorpay-Signature vs Stripe-Signature)
- [ ] Confirm encoding (hex vs base64)
- [ ] Test with provider's webhook testing tool

❓ **Webhooks not arriving?**
- [ ] Verify webhook URL configured in provider dashboard
- [ ] Check firewall/security groups allow provider IPs
- [ ] Ensure HTTPS in production (some providers require)
- [ ] Test with ngrok/localtunnel in development

❓ **Duplicate processing?**
- [ ] Verify unique constraint on provider_event_id
- [ ] Check idempotency logic before save
- [ ] Review database transaction isolation

## Provider-Specific Notes

### Razorpay
- Signature header: `X-Razorpay-Signature`
- Encoding: Hex
- Retry: 5 attempts over 24 hours
- Timeout: 10 seconds
- Events: `payment.captured`, `payment.failed`, `payment.refunded`

### Stripe
- Signature header: `Stripe-Signature`
- Encoding: Base64 with timestamp
- Retry: 3 days with exponential backoff
- Timeout: 5 seconds
- Events: `charge.succeeded`, `charge.failed`, `charge.refunded`

## Security Incident Response

If webhook security is compromised:

1. **Rotate webhook secret immediately** in provider dashboard
2. Update `PAYMENT_WEBHOOK_SECRET` environment variable
3. Review webhook logs for suspicious activity
4. Check for unauthorized payment status changes
5. Notify security team and affected customers
6. Consider temporary webhook endpoint disable while investigating
