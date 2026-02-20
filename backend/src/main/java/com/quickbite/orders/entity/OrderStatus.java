package com.quickbite.orders.entity;

/**
 * Order status enum representing the lifecycle of an order.
 */
public enum OrderStatus {
    PLACED,
    ACCEPTED,
    PREPARING,
    READY,
    ASSIGNED,
    PICKED_UP,
    ENROUTE,
    DELIVERED,
    CANCELLED
}
