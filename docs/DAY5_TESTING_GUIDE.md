# Day 5 Testing Guide - Payment Integration & WebSocket

## Pre-Testing Sanity Checklist

Before starting testing, verify these prerequisites:

- [ ] **Flyway V5 applied** - `webhook_events` table exists in database
- [ ] **PAYMENT_WEBHOOK_SECRET set** - Configured in `application.properties` or environment variables (NOT checked into git)
- [ ] **Backend running** - Available at `http://localhost:8080`
- [ ] **WebSocket accessible** - Frontend can connect to `ws://localhost:8080/ws` (or via ngrok when tunneling)

---

## 1. Simulate Provider Webhook (Bash + curl)

### Generate Test Payload

Create a test webhook payload:

```bash
# payload.json (example)
cat > payload.json <<'JSON'
{
  "id": "evt_test_0001",
  "event": "payment.captured",
  "data": { 
    "payment_id": "mock_payment_12345", 
    "order_id": "order-uuid-here" 
  }
}
JSON
```

### Generate HMAC Signature (Hex - Razorpay style)

```bash
WEBHOOK_SECRET="your_webhook_secret_here"
SIG_HEX=$(printf '%s' "$(cat payload.json)" | \
  openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | xxd -p -c 256)
echo "HEX signature: $SIG_HEX"
```

### Generate HMAC Signature (Base64 - Stripe style)

```bash
SIG_BASE64=$(printf '%s' "$(cat payload.json)" | \
  openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | base64)
echo "BASE64 signature: $SIG_BASE64"
```

### Send Webhook to Backend

```bash
curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: $SIG_HEX" \
  --data-binary @payload.json
```

**For Stripe-style with timestamp:**
```bash
TIMESTAMP=$(date +%s)
SIGNED_PAYLOAD="${TIMESTAMP}.$(cat payload.json)"
SIG_STRIPE=$(printf '%s' "$SIGNED_PAYLOAD" | openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | base64)

curl -X POST http://localhost:8080/api/payments/webhook \
  -H "Content-Type: application/json" \
  -H "Stripe-Signature: t=$TIMESTAMP,v1=$SIG_STRIPE" \
  --data-binary @payload.json
```

---

## 2. SQL Verification Queries

### Check Webhook Events

```sql
-- View recent webhook events
SELECT id, provider_event_id, event_type, processed, processed_at, created_at
FROM webhook_events
ORDER BY created_at DESC
LIMIT 10;

-- Check specific event
SELECT * FROM webhook_events
WHERE provider_event_id = 'evt_test_0001';
```

### Check Payment & Order Status

```sql
-- Verify payment record and linked order
SELECT 
    p.id, 
    p.provider_payment_id, 
    p.status, 
    p.amount_cents, 
    o.id AS order_id, 
    o.payment_status,
    o.status AS order_status
FROM payments p
LEFT JOIN orders o ON o.payment_id = p.id
WHERE p.provider_payment_id = 'mock_payment_12345';
```

### Manual Debug - Mark Event Processed

```sql
-- For local debugging only
UPDATE webhook_events 
SET processed = TRUE, processed_at = NOW()
WHERE provider_event_id = 'evt_test_0001';
```

---

## 3. Test Webhook Idempotency (Duplicate Events)

### Test Flow

1. **Send webhook first time:**
   ```bash
   curl -X POST http://localhost:8080/api/payments/webhook \
     -H "Content-Type: application/json" \
     -H "X-Signature: $SIG_HEX" \
     --data-binary @payload.json
   ```
   **Expected**: `200 OK`, webhook processed

2. **Send same webhook again (duplicate):**
   ```bash
   curl -X POST http://localhost:8080/api/payments/webhook \
     -H "Content-Type: application/json" \
     -H "X-Signature: $SIG_HEX" \
     --data-binary @payload.json
   ```
   **Expected**: `200 OK`, webhook **NOT** processed (idempotent)

3. **Verify in database:**
   ```sql
   SELECT COUNT(*) FROM webhook_events 
   WHERE provider_event_id = 'evt_test_0001';
   ```
   **Expected**: `1` (only one record, unique constraint enforced)

4. **Check backend logs:**
   - Should see: `Webhook event evt_test_0001 already processed (idempotent)`
   - No duplicate payment status updates

---

## 4. Ngrok for Provider Testing

### Setup Ngrok Tunnel

```bash
# Install ngrok (if not already installed)
# Download from https://ngrok.com/download

# Start tunnel
ngrok http 8080

# Output:
# Forwarding  https://abcd1234.ngrok.io -> http://localhost:8080
```

### Configure Provider Webhook URL

**Razorpay:**
- Dashboard → Settings → Webhooks
- URL: `https://abcd1234.ngrok.io/api/payments/webhook`
- Events: `payment.captured`, `payment.failed`, `payment.refunded`
- Save webhook secret

**Stripe:**
- Dashboard → Developers → Webhooks
- Endpoint URL: `https://abcd1234.ngrok.io/api/payments/webhook`
- Events: `charge.succeeded`, `charge.failed`, `charge.refunded`
- Copy webhook signing secret

### Troubleshooting

If host header validation fails:
```bash
ngrok http 8080 -host-header=localhost:8080
```

### Verify Webhook Delivery

1. Trigger test event from provider dashboard
2. Check ngrok web interface: `http://127.0.0.1:4040` (shows all requests)
3. Verify backend logs show webhook received and processed
4. Check database: `SELECT * FROM webhook_events ORDER BY created_at DESC LIMIT 1;`

---

## 5. WireMock Integration Test (Webhook)

### Test Structure

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class WebhookIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private WebhookEventRepository webhookEventRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Value("${payments.webhook.secret}")
    private String webhookSecret;
    
    @Test
    void testWebhookProcessing_Success() {
        // 1. Create test payment in database
        Payment payment = createTestPayment("pay_test_12345");
        
        // 2. Create webhook payload
        String payload = """
            {
                "id": "evt_12345",
                "event": "payment.captured",
                "data": {
                    "payment_id": "pay_test_12345",
                    "status": "captured"
                }
            }
            """;
        
        // 3. Generate HMAC signature
        String signature = WebhookSecurityUtil.computeHmacSha256Hex(payload, webhookSecret);
        
        // 4. Send webhook
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Signature", signature);
        
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/payments/webhook",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );
        
        // 5. Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify webhook event stored
        assertTrue(webhookEventRepository.existsByProviderEventId("evt_12345"));
        
        // Verify payment status updated
        Payment updated = paymentRepository.findByProviderPaymentId("pay_test_12345").orElseThrow();
        assertEquals(PaymentStatus.CAPTURED, updated.getStatus());
    }
    
    @Test
    void testWebhookIdempotency() {
        String payload = """
            {
                "id": "evt_duplicate",
                "event": "payment.captured",
                "data": {"payment_id": "pay_test_99999"}
            }
            """;
        String signature = WebhookSecurityUtil.computeHmacSha256Hex(payload, webhookSecret);
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Signature", signature);
        
        // Send first time
        ResponseEntity<String> response1 = restTemplate.exchange(
            "/api/payments/webhook",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        
        // Send duplicate
        ResponseEntity<String> response2 = restTemplate.exchange(
            "/api/payments/webhook",
            HttpMethod.POST,
            new HttpEntity<>(payload, headers),
            String.class
        );
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        
        // Verify only one event stored
        assertEquals(1, webhookEventRepository.countByProviderEventId("evt_duplicate"));
    }
}
```

---

## 6. React WebSocket Integration

### Install Dependencies

```bash
npm install @stomp/stompjs sockjs-client
```

### Custom Hook: `useOrderUpdates.js`

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useEffect, useRef } from 'react';

export function useOrderUpdates(orderId, onMessage) {
  const clientRef = useRef(null);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => console.debug('[STOMP]', str),
    });

    client.onConnect = () => {
      console.log('WebSocket connected');
      client.subscribe(`/topic/orders.${orderId}`, (msg) => {
        const body = JSON.parse(msg.body);
        console.log('Order update received:', body);
        onMessage(body);
      });
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
    };

    client.onDisconnect = () => {
      console.log('WebSocket disconnected');
    };

    client.activate();
    clientRef.current = client;

    return () => {
      if (clientRef.current) {
        clientRef.current.deactivate();
      }
    };
  }, [orderId, onMessage]);
}
```

### Usage in Component

```javascript
import React, { useState, useCallback } from 'react';
import { useOrderUpdates } from './hooks/useOrderUpdates';

function OrderTracking({ orderId }) {
  const [order, setOrder] = useState(null);

  const handleOrderUpdate = useCallback((payload) => {
    console.log('Order updated:', payload);
    setOrder(payload);
  }, []);

  useOrderUpdates(orderId, handleOrderUpdate);

  if (!order) {
    return <div>Loading order updates...</div>;
  }

  return (
    <div className="order-tracking">
      <h2>Order #{orderId.substring(0, 8)}</h2>
      <div className="status">
        <strong>Status:</strong> {order.status}
      </div>
      <div className="payment">
        <strong>Payment:</strong> {order.paymentStatus}
      </div>
      <div className="total">
        <strong>Total:</strong> ₹{(order.totalCents / 100).toFixed(2)}
      </div>
      <div className="address">
        <strong>Delivery:</strong> {order.deliveryAddress}
      </div>
    </div>
  );
}

export default OrderTracking;
```

### Test WebSocket in Browser Console

```javascript
// Open browser console on http://localhost:5173
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
  debug: (str) => console.log(str)
});

client.onConnect = () => {
  console.log('Connected!');
  client.subscribe('/topic/orders.{YOUR_ORDER_ID}', (message) => {
    console.log('Received:', JSON.parse(message.body));
  });
};

client.activate();
```

---

## 7. Manual End-to-End Test (Payment + WebSocket)

### Test Flow

1. **Create Order (Customer)**
   ```bash
   curl -X POST http://localhost:8080/api/orders \
     -H "Authorization: Bearer $CUSTOMER_JWT" \
     -H "Content-Type: application/json" \
     -d '{
       "items": [{"menuItemId": "...", "quantity": 2}],
       "addressId": "...",
       "paymentMethod": "CARD"
     }'
   ```
   **Response**: Order created with `orderId`

2. **Subscribe to WebSocket (Frontend)**
   ```javascript
   useOrderUpdates(orderId, (update) => {
     console.log('Real-time update:', update);
   });
   ```

3. **Create Payment Intent**
   ```bash
   curl -X POST http://localhost:8080/api/payments/intent \
     -H "Authorization: Bearer $CUSTOMER_JWT" \
     -H "Content-Type: application/json" \
     -d '{
       "orderId": "...",
       "currency": "INR"
     }'
   ```
   **Response**: `providerPaymentId`, `clientSecret`

4. **Simulate Provider Webhook (Payment Captured)**
   ```bash
   # Create payload with actual order/payment IDs
   cat > webhook_captured.json <<JSON
   {
     "id": "evt_captured_001",
     "event": "payment.captured",
     "data": {
       "payment_id": "$PROVIDER_PAYMENT_ID",
       "status": "captured"
     }
   }
   JSON
   
   # Generate signature
   SIG=$(printf '%s' "$(cat webhook_captured.json)" | \
     openssl dgst -sha256 -hmac "$WEBHOOK_SECRET" -binary | xxd -p -c 256)
   
   # Send webhook
   curl -X POST http://localhost:8080/api/payments/webhook \
     -H "X-Signature: $SIG" \
     --data-binary @webhook_captured.json
   ```

5. **Observe WebSocket Update**
   - Frontend should receive order update with `paymentStatus: "CAPTURED"`
   - Console: `Real-time update: { orderId: "...", status: "PLACED", paymentStatus: "CAPTURED" }`

6. **Verify in Database**
   ```sql
   SELECT o.id, o.status, o.payment_status, p.status AS payment_status_detail
   FROM orders o
   JOIN payments p ON o.payment_id = p.id
   WHERE o.id = 'YOUR_ORDER_ID';
   ```

---

## 8. Prioritized Next Steps

### Priority 1 - Production Safety 🔴

**Critical for production deployment:**

- [ ] **WebSocket Handshake Auth** - Add JWT token validation on WebSocket connection
  ```java
  @Configuration
  public class WebSocketSecurityConfig {
      @Override
      public void configureClientInboundChannel(ChannelRegistration registration) {
          registration.interceptors(new JwtChannelInterceptor());
      }
  }
  ```

- [ ] **Rate Limiting** - Protect webhook endpoint from abuse
  ```java
  @RateLimiter(name = "webhook", fallbackMethod = "webhookRateLimitFallback")
  @PostMapping("/webhook")
  public ResponseEntity<?> webhook() { }
  ```

- [ ] **Secrets Management** - Move `PAYMENT_WEBHOOK_SECRET` to AWS Secrets Manager / Vault
  ```java
  @ConfigurationProperties(prefix = "payments")
  public class PaymentProperties {
      @Value("${aws.secretsmanager.payments.webhook.secret}")
      private String webhookSecret;
  }
  ```

- [ ] **External Message Broker** - Switch to Redis/RabbitMQ for WebSocket scaling
  ```java
  @Override
  public void configureMessageBroker(MessageBrokerRegistry config) {
      config.enableStompBrokerRelay("/topic")
            .setRelayHost("rabbitmq.example.com")
            .setRelayPort(61613);
  }
  ```

### Priority 2 - Provider Integration 🟡

**Replace stubs with real SDKs:**

- [ ] **Add Provider SDKs** to pom.xml
  ```xml
  <!-- Razorpay -->
  <dependency>
      <groupId>com.razorpay</groupId>
      <artifactId>razorpay-java</artifactId>
      <version>1.4.3</version>
  </dependency>
  
  <!-- Stripe -->
  <dependency>
      <groupId>com.stripe</groupId>
      <artifactId>stripe-java</artifactId>
      <version>24.0.0</version>
  </dependency>
  ```

- [ ] **Replace Stub Methods** in `PaymentService`
  ```java
  private String createProviderPaymentIntent(Long amountCents, String currency) {
      if ("razorpay".equals(provider)) {
          RazorpayClient client = new RazorpayClient(apiKey, apiSecret);
          JSONObject orderRequest = new JSONObject();
          orderRequest.put("amount", amountCents);
          orderRequest.put("currency", currency);
          Order order = client.Orders.create(orderRequest);
          return order.get("id");
      }
      // ... Stripe implementation
  }
  ```

- [ ] **Idempotent Outgoing Calls** - Use `Idempotency-Key` header
  ```java
  headers.set("Idempotency-Key", UUID.randomUUID().toString());
  ```

### Priority 3 - Testing & Monitoring 🟢

**Improve reliability and observability:**

- [ ] **WireMock Webhook Tests** - As shown in section 5
- [ ] **WebSocket Load Test** (k6)
  ```javascript
  import ws from 'k6/ws';
  export default function () {
    ws.connect('ws://localhost:8080/ws', function (socket) {
      socket.on('open', () => socket.send('CONNECT'));
      socket.on('message', (data) => console.log(data));
    });
  }
  ```

- [ ] **Monitoring Metrics**
  ```java
  @Autowired
  private MeterRegistry meterRegistry;
  
  meterRegistry.counter("webhook.processed", "event_type", eventType).increment();
  meterRegistry.counter("payment.status", "status", status.name()).increment();
  ```

- [ ] **Alert Setup** - Slack/PagerDuty for:
  - Webhook processing failures
  - Payment reconciliation mismatches
  - WebSocket connection spikes

### Priority 4 - UX & Features 💙

**Nice-to-have enhancements:**

- [ ] **Real-time Driver Location** - Add GPS tracking via WebSocket
  ```java
  messagingTemplate.convertAndSend("/topic/driver." + driverId, locationUpdate);
  ```

- [ ] **Webhook Retry/Reconciliation** - Handle missed events
- [ ] **Webhook Replay UI** - Ops dashboard to replay failed webhooks
- [ ] **Partial Refunds** - Business logic for partial amount refunds

---

## Quick Reference Commands

### Start Backend
```bash
cd backend
mvn spring-boot:run
```

### Connect to Database
```bash
psql -h localhost -U dbuser -d quickbite
```

### Check Backend Health
```bash
curl http://localhost:8080/actuator/health
```

### View Backend Logs
```bash
tail -f backend/logs/quickbite.log
# or if using console logs
mvn spring-boot:run | grep -i webhook
```

### Generate Test JWT (for API calls)
```bash
# Use your existing auth endpoint
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "customer@example.com", "password": "password"}' \
  | jq -r '.accessToken'
```

---

## Troubleshooting

### Webhook Signature Verification Fails

**Symptoms**: `401 Unauthorized` or logs show "signature verification failed"

**Solutions**:
1. Verify `PAYMENT_WEBHOOK_SECRET` matches provider dashboard
2. Check raw body is used (not parsed JSON)
3. Verify signature header name (`X-Signature` vs `X-Razorpay-Signature`)
4. Test with known-good signature using command in section 1

### WebSocket Connection Fails

**Symptoms**: Frontend shows "disconnected", browser console errors

**Solutions**:
1. Check CORS configuration: `spring.websocket.allowed-origins`
2. Verify `/ws` endpoint accessible: `curl http://localhost:8080/ws`
3. Try SockJS fallback URL: `http://localhost:8080/ws/info`
4. Check firewall/antivirus blocking WebSocket

### Webhook Not Processed

**Symptoms**: `webhook_events` empty, payment status not updated

**Solutions**:
1. Check backend logs for exceptions
2. Verify database migration V5 applied: `SELECT * FROM flyway_schema_history WHERE version = '5';`
3. Test with curl command from section 1
4. Check `WebhookEvent` save transaction not rolled back

### WebSocket Updates Not Received

**Symptoms**: Order status changes but frontend doesn't update

**Solutions**:
1. Verify `OrderUpdatePublisher` is called: add debug logs
2. Check subscription destination matches: `/topic/orders.{orderId}`
3. Confirm WebSocket connection established before status change
4. Try manual send: `messagingTemplate.convertAndSend("/topic/test", "test")`

---

## Success Criteria

✅ **Testing Complete** when:

- [ ] Webhook signature verification passes for all providers
- [ ] Webhook idempotency prevents duplicate processing
- [ ] Payment status updates reflect in database
- [ ] Order status updates broadcast via WebSocket
- [ ] Frontend receives real-time updates
- [ ] ngrok webhook delivery works with real provider
- [ ] Integration tests pass
- [ ] No errors in backend logs during test flows

**Next**: Ready for staging deployment and real provider integration! 🚀
