package com.quickbite.websocket;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for publishing real-time order updates to WebSocket clients.
 * Broadcasts order status changes to subscribed clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderUpdatePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Publish order update to WebSocket channel.
     *
     * @param order updated order
     */
    public void publishOrderUpdate(Order order) {
        try {
            OrderUpdateDTO update = mapToDTO(order);
            
            // Send to order-specific channel: /topic/orders.{orderId}
            String destination = "/topic/orders." + order.getId();
            messagingTemplate.convertAndSend(destination, update);
            
            log.debug("Published order update to {}: status={}", destination, order.getStatus());
            
        } catch (Exception e) {
            log.error("Failed to publish order update for order {}", order.getId(), e);
        }
    }

    /**
     * Publish status change event.
     *
     * @param orderId order ID
     * @param status new status
     * @param message optional message
     */
    public void publishStatusChange(UUID orderId, OrderStatus status, String message) {
        try {
            OrderStatusChangeDTO statusChange = OrderStatusChangeDTO.builder()
                    .orderId(orderId)
                    .status(status)
                    .message(message)
                    .timestamp(OffsetDateTime.now())
                    .build();

            String destination = "/topic/orders." + orderId;
            messagingTemplate.convertAndSend(destination, statusChange);
            
            log.debug("Published status change to {}: status={}", destination, status);
            
        } catch (Exception e) {
            log.error("Failed to publish status change for order {}", orderId, e);
        }
    }

    /**
     * Map Order entity to DTO for WebSocket transmission.
     */
    private OrderUpdateDTO mapToDTO(Order order) {
        String deliveryAddressText = order.getDeliveryAddress() != null 
            ? order.getDeliveryAddress().getLine1() + ", " + order.getDeliveryAddress().getCity()
            : null;
            
        return OrderUpdateDTO.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .totalCents(order.getTotalCents())
                .deliveryAddress(deliveryAddressText)
                .scheduledTime(order.getScheduledTime())
                .deliveredAt(order.getDeliveredAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    /**
     * DTO for order update events.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderUpdateDTO {
        private UUID orderId;
        private OrderStatus status;
        private com.quickbite.payments.entity.PaymentStatus paymentStatus;
        private Long totalCents;
        private String deliveryAddress;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime scheduledTime;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime deliveredAt;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime updatedAt;
    }

    /**
     * DTO for simple status change notifications.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderStatusChangeDTO {
        private UUID orderId;
        private OrderStatus status;
        private String message;
        
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
        private OffsetDateTime timestamp;
    }
}
