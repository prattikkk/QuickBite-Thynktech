package com.quickbite.payments.entity;

/**
 * Payment status enum representing payment states.
 */
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    CANCELLED
}
