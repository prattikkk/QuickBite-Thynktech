# WebSocket Integration - Real-time Order Tracking

## Overview

QuickBite uses **WebSocket with STOMP protocol** for real-time order status updates. This enables:
- Live order status updates without polling
- Instant notifications to customers, vendors, and drivers
- Reduced server load compared to polling
- Better user experience with real-time feedback

## Architecture

```
┌─────────────┐                    ┌──────────────┐
│   React     │◄───WebSocket──────►│ Spring Boot  │
│  Frontend   │   (STOMP/SockJS)   │   Backend    │
└─────────────┘                    └──────┬───────┘
       │                                   │
       │ Subscribe to:                     │
       │ /topic/orders.{orderId}           │
       │                                   │
       │                           ┌───────▼────────┐
       │                           │ OrderService   │
       │                           │ - updateStatus │
       │                           │ - acceptOrder  │
       │                           └───────┬────────┘
       │                                   │
       │                           ┌───────▼────────────┐
       │◄─────Broadcast────────────│OrderUpdatePublisher│
       │                           │ - publishUpdate    │
                                   └────────────────────┘
```

## Backend Configuration

### 1. WebSocket Config

**File:** `com.quickbite.websocket.config.WebSocketConfig`

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // Subscribe to /topic/*
        config.setApplicationDestinationPrefixes("/app");  // Send to /app/*
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // Configure for production
                .withSockJS();  // Fallback for browsers without WebSocket
    }
}
```

### 2. Order Update Publisher

**File:** `com.quickbite.websocket.OrderUpdatePublisher`

```java
@Service
@RequiredArgsConstructor
public class OrderUpdatePublisher {
    
    private final SimpMessagingTemplate messagingTemplate;

    public void publishOrderUpdate(Order order) {
        OrderUpdateDTO update = mapToDTO(order);
        String destination = "/topic/orders." + order.getId();
        messagingTemplate.convertAndSend(destination, update);
    }

    public void publishStatusChange(UUID orderId, OrderStatus status, String message) {
        OrderStatusChangeDTO change = OrderStatusChangeDTO.builder()
            .orderId(orderId)
            .status(status)
            .message(message)
            .timestamp(OffsetDateTime.now())
            .build();
        
        messagingTemplate.convertAndSend("/topic/orders." + orderId, change);
    }
}
```

### 3. Integration with OrderService

```java
@Service
public class OrderService {
    
    private final OrderUpdatePublisher orderUpdatePublisher;
    
    @Transactional
    public OrderResponseDTO updateOrderStatus(UUID orderId, StatusUpdateDTO dto) {
        // ... update order logic ...
        
        order = orderRepository.save(order);
        
        // Publish real-time update
        orderUpdatePublisher.publishOrderUpdate(order);
        
        return orderMapper.toResponseDTO(order);
    }
}
```

### Message DTOs

**OrderUpdateDTO:**
```java
{
  "orderId": "uuid",
  "status": "PREPARING",
  "paymentStatus": "CAPTURED",
  "deliveryStatus": "PENDING",
  "totalCents": 50000,
  "deliveryAddress": "123 Main St",
  "estimatedDeliveryTime": "2024-01-15T14:30:00Z",
  "updatedAt": "2024-01-15T13:45:00Z"
}
```

**OrderStatusChangeDTO:**
```java
{
  "orderId": "uuid",
  "status": "ENROUTE",
  "message": "Driver is on the way",
  "timestamp": "2024-01-15T14:15:00Z"
}
```

## Frontend Integration (React)

### 1. Install Dependencies

```bash
npm install @stomp/stompjs sockjs-client
```

### 2. WebSocket Hook

**File:** `src/hooks/useOrderTracking.ts`

```typescript
import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

interface OrderUpdate {
  orderId: string;
  status: string;
  paymentStatus: string;
  deliveryStatus: string;
  totalCents: number;
  deliveryAddress: string;
  estimatedDeliveryTime: string;
  updatedAt: string;
}

export function useOrderTracking(orderId: string) {
  const [orderUpdate, setOrderUpdate] = useState<OrderUpdate | null>(null);
  const [connected, setConnected] = useState(false);
  const stompClient = useRef<Client | null>(null);

  useEffect(() => {
    // Create STOMP client with SockJS
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      debug: (str) => console.log('STOMP:', str),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // Connection established
    client.onConnect = () => {
      console.log('WebSocket connected');
      setConnected(true);

      // Subscribe to order-specific channel
      client.subscribe(`/topic/orders.${orderId}`, (message) => {
        const update: OrderUpdate = JSON.parse(message.body);
        console.log('Order update received:', update);
        setOrderUpdate(update);
      });
    };

    // Connection error
    client.onStompError = (frame) => {
      console.error('STOMP error:', frame);
      setConnected(false);
    };

    // Disconnect handler
    client.onDisconnect = () => {
      console.log('WebSocket disconnected');
      setConnected(false);
    };

    // Activate connection
    client.activate();
    stompClient.current = client;

    // Cleanup on unmount
    return () => {
      if (stompClient.current) {
        stompClient.current.deactivate();
      }
    };
  }, [orderId]);

  return { orderUpdate, connected };
}
```

### 3. Order Tracking Component

**File:** `src/pages/OrderTracking.tsx`

```typescript
import React from 'react';
import { useParams } from 'react-router-dom';
import { useOrderTracking } from '../hooks/useOrderTracking';

export function OrderTracking() {
  const { orderId } = useParams<{ orderId: string }>();
  const { orderUpdate, connected } = useOrderTracking(orderId!);

  return (
    <div className="order-tracking">
      <h1>Order Tracking</h1>
      
      {/* Connection Status */}
      <div className={`status ${connected ? 'connected' : 'disconnected'}`}>
        {connected ? '🟢 Live' : '🔴 Connecting...'}
      </div>

      {/* Order Status */}
      {orderUpdate && (
        <div className="order-status">
          <h2>Order #{orderId.substring(0, 8)}</h2>
          
          <div className="status-timeline">
            <StatusStep 
              label="Placed" 
              active={orderUpdate.status !== 'CANCELLED'} 
              completed 
            />
            <StatusStep 
              label="Accepted" 
              active={['ACCEPTED', 'PREPARING', 'READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
              completed={['PREPARING', 'READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
            />
            <StatusStep 
              label="Preparing" 
              active={['PREPARING', 'READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
              completed={['READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
            />
            <StatusStep 
              label="Ready" 
              active={['READY', 'PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
              completed={['PICKED_UP', 'ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
            />
            <StatusStep 
              label="Enroute" 
              active={['ENROUTE', 'DELIVERED'].includes(orderUpdate.status)}
              completed={orderUpdate.status === 'DELIVERED'}
            />
            <StatusStep 
              label="Delivered" 
              active={orderUpdate.status === 'DELIVERED'}
              completed={orderUpdate.status === 'DELIVERED'}
            />
          </div>

          {/* Order Details */}
          <div className="order-details">
            <p><strong>Status:</strong> {orderUpdate.status}</p>
            <p><strong>Payment:</strong> {orderUpdate.paymentStatus}</p>
            <p><strong>Total:</strong> ₹{(orderUpdate.totalCents / 100).toFixed(2)}</p>
            <p><strong>Delivery Address:</strong> {orderUpdate.deliveryAddress}</p>
            {orderUpdate.estimatedDeliveryTime && (
              <p><strong>ETA:</strong> {new Date(orderUpdate.estimatedDeliveryTime).toLocaleTimeString()}</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function StatusStep({ label, active, completed }: { label: string; active: boolean; completed: boolean }) {
  return (
    <div className={`status-step ${active ? 'active' : ''} ${completed ? 'completed' : ''}`}>
      <div className="step-icon">{completed ? '✓' : '○'}</div>
      <div className="step-label">{label}</div>
    </div>
  );
}
```

### 4. Styling

**File:** `src/styles/OrderTracking.css`

```css
.order-tracking {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.status {
  display: inline-block;
  padding: 5px 10px;
  border-radius: 5px;
  font-weight: bold;
  margin-bottom: 20px;
}

.status.connected {
  background: #d4edda;
  color: #155724;
}

.status.disconnected {
  background: #f8d7da;
  color: #721c24;
}

.status-timeline {
  display: flex;
  justify-content: space-between;
  margin: 40px 0;
  position: relative;
}

.status-timeline::before {
  content: '';
  position: absolute;
  top: 20px;
  left: 0;
  right: 0;
  height: 2px;
  background: #ddd;
  z-index: -1;
}

.status-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-bottom: 10px;
}

.status-step.active .step-icon {
  background: #ffc107;
  color: white;
}

.status-step.completed .step-icon {
  background: #28a745;
  color: white;
}

.order-details {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-top: 20px;
}

.order-details p {
  margin: 10px 0;
}
```

## Testing

### 1. Backend Test

```java
@SpringBootTest
@AutoConfigureWebSocket
class WebSocketIntegrationTest {
    
    @Autowired
    private OrderUpdatePublisher publisher;
    
    @Test
    void testOrderUpdateBroadcast() throws Exception {
        // Create test order
        Order order = createTestOrder();
        
        // Set up WebSocket test client
        StompSession session = connectToWebSocket();
        
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<OrderUpdateDTO> received = new AtomicReference<>();
        
        session.subscribe("/topic/orders." + order.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return OrderUpdateDTO.class;
            }
            
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.set((OrderUpdateDTO) payload);
                latch.countDown();
            }
        });
        
        // Publish update
        publisher.publishOrderUpdate(order);
        
        // Verify received
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(order.getId(), received.get().getOrderId());
    }
}
```

### 2. Frontend Test

Use browser DevTools Console:

```javascript
// Connect to WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = new StompJS.Client({
  webSocketFactory: () => socket,
  debug: (str) => console.log(str)
});

stompClient.onConnect = () => {
  console.log('Connected');
  
  // Subscribe to order updates
  stompClient.subscribe('/topic/orders.{orderId}', (message) => {
    console.log('Received:', JSON.parse(message.body));
  });
};

stompClient.activate();
```

## Scaling Considerations

### 1. Multiple Backend Instances

For load-balanced deployments, use external message broker:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Use RabbitMQ/Redis instead of in-memory broker
        config.enableStompBrokerRelay("/topic")
              .setRelayHost("rabbitmq.example.com")
              .setRelayPort(61613)
              .setClientLogin("guest")
              .setClientPasscode("guest");
    }
}
```

**Dependencies:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-reactor-netty</artifactId>
</dependency>
```

### 2. Redis Pub/Sub Alternative

For simpler scaling without STOMP relay:

```java
@Service
public class RedisOrderPublisher {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void publishOrderUpdate(Order order) {
        String channel = "orders." + order.getId();
        OrderUpdateDTO dto = mapToDTO(order);
        redisTemplate.convertAndSend(channel, dto);
    }
}
```

### 3. WebSocket Session Management

Track active connections:

```java
@Component
public class WebSocketEventListener {
    
    private final Map<String, Set<String>> orderSubscriptions = new ConcurrentHashMap<>();
    
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        String destination = extractDestination(event);
        String sessionId = event.getMessage().getHeaders().get("simpSessionId").toString();
        
        orderSubscriptions.computeIfAbsent(destination, k -> new HashSet<>())
                         .add(sessionId);
        
        log.info("Session {} subscribed to {}", sessionId, destination);
    }
    
    @EventListener
    public void handleDisconnectEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        orderSubscriptions.values().forEach(set -> set.remove(sessionId));
        log.info("Session {} disconnected", sessionId);
    }
}
```

## Production Checklist

- [ ] Configure CORS properly (no wildcard `*` in production)
- [ ] Set up external message broker (RabbitMQ/Redis) for multi-instance
- [ ] Enable WebSocket compression
- [ ] Configure heartbeat intervals
- [ ] Set up monitoring for active connections
- [ ] Implement reconnection logic with exponential backoff
- [ ] Add authentication to WebSocket handshake
- [ ] Configure WebSocket timeout settings
- [ ] Test with load balancer (sticky sessions or STOMP relay)
- [ ] Monitor memory usage with many concurrent connections

## Troubleshooting

### Connection Fails
- Check CORS configuration
- Verify `/ws` endpoint accessible
- Test SockJS fallback if WebSocket blocked
- Check firewall rules

### Messages Not Received
- Verify subscription destination matches publish destination
- Check message broker configuration
- Ensure order ID format is correct
- Review backend logs for errors

### Reconnection Issues
- Implement exponential backoff
- Add connection state management
- Handle stale subscriptions on reconnect

## Alternative: Server-Sent Events (SSE)

For simpler one-way communication (server → client only):

```java
@GetMapping(value = "/orders/{orderId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<OrderUpdateDTO> streamOrderUpdates(@PathVariable UUID orderId) {
    return orderUpdateService.streamUpdates(orderId);
}
```

```typescript
const eventSource = new EventSource(`/api/orders/${orderId}/stream`);
eventSource.onmessage = (event) => {
  const update = JSON.parse(event.data);
  console.log('Order update:', update);
};
```

**Pros:** Simpler, no library needed, automatic reconnect
**Cons:** One-way only, less efficient than WebSocket for high-frequency updates
