package com.quickbite.orders.exception;

import java.util.UUID;

/**
 * Exception thrown when order is not found.
 */
public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }

    public OrderNotFoundException(String message) {
        super(message);
    }
}
