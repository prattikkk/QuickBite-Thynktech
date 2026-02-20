package com.quickbite.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time order tracking.
 * Enables STOMP over WebSocket with SockJS fallback.
 * 
 * Clients can connect to: ws://localhost:8080/ws
 * Subscribe to channels: /topic/orders.{orderId}
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for /topic prefix
        config.enableSimpleBroker("/topic");
        
        // Application destination prefix for client messages
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint with SockJS fallback (for browser clients)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Configure properly in production
                .withSockJS();

        // Register plain WebSocket endpoint (for native WS clients like Node.js tests)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }
}
