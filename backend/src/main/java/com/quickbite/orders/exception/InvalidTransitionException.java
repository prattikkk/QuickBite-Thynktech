package com.quickbite.orders.exception;

/**
 * Thrown when an invalid order status transition is attempted.
 */
public class InvalidTransitionException extends RuntimeException {

    private final String currentStatus;
    private final String targetStatus;
    private final String reason;

    public InvalidTransitionException(String currentStatus, String targetStatus, String reason) {
        super(String.format("Invalid transition %s → %s: %s", currentStatus, targetStatus, reason));
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
        this.reason = reason;
    }

    public InvalidTransitionException(String currentStatus, String targetStatus) {
        this(currentStatus, targetStatus, "Transition not allowed");
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public String getReason() {
        return reason;
    }
}
