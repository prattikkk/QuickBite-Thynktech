package com.quickbite.orders.entity;

/**
 * Whether the customer picks up the order or requires delivery.
 */
public enum DeliveryType {
    /** Customer picks up from the restaurant */
    PICKUP,
    /** Order is delivered to the customer's address */
    DELIVERY
}
