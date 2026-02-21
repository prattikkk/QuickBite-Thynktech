package com.quickbite.promotions.entity;

/**
 * Discount type for promo codes.
 */
public enum DiscountType {
    /** Fixed amount off in cents (e.g. 5000 = ₹50 off) */
    FIXED,
    /** Percentage off in basis points (e.g. 1500 = 15% off) */
    PERCENT
}
