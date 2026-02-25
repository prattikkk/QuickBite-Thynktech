package com.quickbite.common.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Phase 4.9/4.10: Centralized business metrics for Grafana dashboards.
 * Registers custom counters and timers for key business events.
 */
@Getter
@Component
public class BusinessMetrics {

    private final Counter ordersPlaced;
    private final Counter ordersDelivered;
    private final Counter ordersCancelled;
    private final Counter paymentsSuccessful;
    private final Counter paymentsFailed;
    private final Counter chatMessagesSent;
    private final Counter driverAssignments;
    private final Counter driverAssignmentFailures;
    private final Timer orderDeliveryTime;
    private final Timer orderPrepTime;

    public BusinessMetrics(MeterRegistry registry) {
        ordersPlaced = Counter.builder("quickbite.orders.placed")
                .description("Total orders placed")
                .register(registry);

        ordersDelivered = Counter.builder("quickbite.orders.delivered")
                .description("Total orders delivered")
                .register(registry);

        ordersCancelled = Counter.builder("quickbite.orders.cancelled")
                .description("Total orders cancelled")
                .register(registry);

        paymentsSuccessful = Counter.builder("quickbite.payments.successful")
                .description("Total successful payments")
                .register(registry);

        paymentsFailed = Counter.builder("quickbite.payments.failed")
                .description("Total failed payments")
                .register(registry);

        chatMessagesSent = Counter.builder("quickbite.chat.messages.sent")
                .description("Total chat messages sent")
                .register(registry);

        driverAssignments = Counter.builder("quickbite.dispatch.assignments")
                .description("Total driver assignments")
                .register(registry);

        driverAssignmentFailures = Counter.builder("quickbite.dispatch.failures")
                .description("Total driver assignment failures")
                .register(registry);

        orderDeliveryTime = Timer.builder("quickbite.orders.delivery.time")
                .description("Order delivery duration")
                .register(registry);

        orderPrepTime = Timer.builder("quickbite.orders.prep.time")
                .description("Order preparation duration")
                .register(registry);
    }
}
