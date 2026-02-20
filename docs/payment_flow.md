# Payment Integration Flow

## Overview

QuickBite supports multiple payment providers (Razorpay, Stripe) with a provider-agnostic architecture.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────┐
│  Frontend   │────▶│ PaymentCtrl  │────▶│ PaymentService  │
│   (React)   │     │  (Backend)   │     │                 │
└─────────────┘     └──────────────┘     └─────────────────┘
       │                                          │
       │                                          ▼
       │                                  ┌───────────────┐
       │                                  │   Provider    │
       └─────────────────────────────────▶│ (Razorpay/    │
                (Client SDK)              │  Stripe)      │
                                          └───────────────┘
                                                  │
                                                  │ Webhook
                                                  ▼
                                          ┌───────────────┐
                                          │  /webhook     │
                                          │  (HMAC auth)  │
                                          └───────────────┘
```

## Payment Flow

### 1. Create Payment Intent (Order Placement)

**Client → Backend:**
```http
POST /api/payments/intent
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "orderId": "uuid",
  "currency": "INR",
  "paymentMethod": "CARD",
  "description": "Order #ORD-123"
}
```

**Backend → Provider:**
```java
// Razorpay Example
RazorpayClient razorpayClient = new RazorpayClient(apiKey, apiSecret);
JSONObject orderRequest = new JSONObject();
orderRequest.put("amount", amountCents);
orderRequest.put("currency", "INR");
orderRequest.put("receipt", orderId.toString());
Order order = razorpayClient.Orders.create(orderRequest);
```

**Backend Response:**
```json
{
  "paymentId": "uuid",
  "providerPaymentId": "order_Mab123xyz",
  "clientSecret": "rzp_test_secret_key",
  "amountCents": 50000,
  "currency": "INR",
  "status": "PENDING"
}
```

### 2. Client-side Payment Confirmation

**Razorpay Integration:**
```javascript
const options = {
  key: 'rzp_test_key',
  amount: response.amountCents,
  currency: response.currency,
  order_id: response.providerPaymentId,
  handler: function (razorpayResponse) {
    // Payment successful
    console.log('Payment ID:', razorpayResponse.razorpay_payment_id);
    console.log('Order ID:', razorpayResponse.razorpay_order_id);
    console.log('Signature:', razorpayResponse.razorpay_signature);
    
    // Verify payment with backend via webhook
  },
  prefill: {
    name: 'John Doe',
    email: 'john@example.com',
    contact: '9999999999'
  }
};

const rzp = new Razorpay(options);
rzp.open();
```

**Stripe Integration:**
```javascript
const stripe = Stripe('pk_test_key');
const { error } = await stripe.confirmCardPayment(clientSecret, {
  payment_method: {
    card: cardElement,
    billing_details: { name: 'John Doe' }
  }
});

if (error) {
  console.error('Payment failed:', error);
} else {
  console.log('Payment successful');
}
```

### 3. Webhook Confirmation (Asynchronous)

**Provider → Backend:**
```http
POST /api/payments/webhook
X-Razorpay-Signature: {hmac_signature}
Content-Type: application/json

{
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_Mab123xyz",
        "order_id": "order_Mab123xyz",
        "status": "captured",
        "amount": 50000
      }
    }
  }
}
```

**Backend Processing:**
1. Verify HMAC signature
2. Check idempotency (webhook_events table)
3. Update payment status in database
4. Update order payment_status
5. Trigger order state transition (if applicable)
6. Send notification to customer/vendor
7. Publish WebSocket event for real-time UI update

## Payment States

```
PENDING → AUTHORIZED → CAPTURED
   ↓            ↓
FAILED      REFUNDED
```

## Provider Configuration

### Razorpay Setup

1. Sign up at https://razorpay.com
2. Get API keys from Dashboard → Settings → API Keys
3. Configure webhook URL: `https://yourdomain.com/api/payments/webhook`
4. Select events: `payment.captured`, `payment.failed`, `payment.refunded`

**Environment Variables:**
```bash
PAYMENT_PROVIDER=razorpay
PAYMENT_API_KEY=rzp_test_xxxxx
PAYMENT_API_SECRET=your_secret_key
PAYMENT_WEBHOOK_SECRET=webhook_secret_from_dashboard
PAYMENT_WEBHOOK_HEADER=X-Razorpay-Signature
```

### Stripe Setup

1. Sign up at https://stripe.com
2. Get API keys from Dashboard → Developers → API Keys
3. Configure webhook endpoint: `https://yourdomain.com/api/payments/webhook`
4. Select events: `charge.succeeded`, `charge.failed`, `charge.refunded`

**Environment Variables:**
```bash
PAYMENT_PROVIDER=stripe
PAYMENT_API_KEY=pk_test_xxxxx
PAYMENT_API_SECRET=sk_test_xxxxx
PAYMENT_WEBHOOK_SECRET=whsec_xxxxx
PAYMENT_WEBHOOK_HEADER=Stripe-Signature
```

## Error Handling

### Client-side Errors
- Payment declined by user
- Insufficient funds
- Card verification failed
- Network errors

### Server-side Errors
- Provider API timeout
- Webhook signature mismatch
- Duplicate webhook events (handled via idempotency)
- Payment amount mismatch

## Capture vs. Authorize

### Authorize (Card/UPI)
Payment amount is blocked but not charged. Captured later on order delivery.

```java
// Create with capture=false
payment.setStatus(PaymentStatus.AUTHORIZED);

// Capture on delivery
paymentService.capturePayment(providerPaymentId, amountCents);
```

### Direct Capture (Wallets)
Payment is immediately captured on order placement.

## Refunds

Automatic refund on order cancellation:

```java
@Transactional
public void cancelOrder(UUID orderId) {
    Order order = orderRepository.findById(orderId).orElseThrow();
    
    if (order.getPaymentStatus() == PaymentStatus.CAPTURED) {
        paymentService.refundPayment(
            order.getPayment().getProviderPaymentId(),
            null, // null = full refund
            "Order cancelled by customer"
        );
    }
    
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);
}
```

## Testing

### Test Mode Credentials
- **Razorpay:** Use test keys (rzp_test_*)
- **Stripe:** Use test keys (pk_test_*, sk_test_*)

### Test Cards
**Razorpay:**
- Success: `4111 1111 1111 1111`
- Failure: `4000 0000 0000 0002`

**Stripe:**
- Success: `4242 4242 4242 4242`
- Declined: `4000 0000 0000 0002`
- Requires Auth: `4000 0025 0000 3155`

### Webhook Testing
Use ngrok or localtunnel for local testing:
```bash
ngrok http 8080
# Use https://xxxx.ngrok.io/api/payments/webhook as webhook URL
```

## Security Considerations

1. **Never expose API secrets to frontend** - Only use public keys (pk_*, rzp_test_*)
2. **Always verify webhook signatures** - Prevent replay attacks
3. **Implement idempotency** - Store webhook event IDs
4. **Use HTTPS in production** - Protect sensitive payment data
5. **Validate amounts server-side** - Never trust client-provided amounts
6. **Log all payment operations** - For audit and debugging
7. **Set webhook timeout** - Respond within 5 seconds to prevent retries

## Migration from Stub to Real Provider

1. Add provider SDK dependency to pom.xml:
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

2. Replace stub methods in PaymentService:
   - `createProviderPaymentIntent()` → Real SDK call
   - `captureProviderPayment()` → Real SDK call
   - `refundProviderPayment()` → Real SDK call

3. Configure provider credentials in application.properties or environment variables

4. Test with provider's test mode before going live

## Production Checklist

- [ ] Configure real provider credentials
- [ ] Set PAYMENT_WEBHOOK_SECRET securely
- [ ] Enable HTTPS
- [ ] Configure CORS properly
- [ ] Set up webhook monitoring/alerting
- [ ] Test refund flow end-to-end
- [ ] Document payment reconciliation process
- [ ] Set up automated payment verification cron job
- [ ] Configure provider webhook retry policy
- [ ] Test failure scenarios (network timeout, provider downtime)
