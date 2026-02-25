package com.quickbite.orders.service;

import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Basic fraud velocity checks for order creation.
 * Validates order frequency and spend patterns against configurable thresholds.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFraudService {

    private final OrderRepository orderRepository;

    @Value("${fraud.max-orders-per-hour:5}")
    private int maxOrdersPerHour;

    @Value("${fraud.max-spend-per-day-cents:5000000}")
    private long maxSpendPerDayCents;

    @Value("${fraud.max-cancelled-per-day:5}")
    private int maxCancelledPerDay;

    @Value("${fraud.enabled:true}")
    private boolean fraudCheckEnabled;

    /**
     * Perform fraud velocity checks before creating an order.
     * Returns a list of warnings/blocks. Empty list = order is safe.
     */
    @Transactional(readOnly = true)
    public FraudCheckResult checkOrderCreation(UUID customerId, long orderTotalCents) {
        if (!fraudCheckEnabled) {
            return FraudCheckResult.safe();
        }

        List<String> warnings = new ArrayList<>();
        boolean blocked = false;

        // Check 1: Orders per hour
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        long recentOrders = orderRepository.countByCustomerIdAndCreatedAtAfter(customerId, oneHourAgo);
        if (recentOrders >= maxOrdersPerHour) {
            warnings.add("Too many orders in the last hour (" + recentOrders + "/" + maxOrdersPerHour + ")");
            blocked = true;
            log.warn("FRAUD: Customer {} exceeded max orders/hour: {}", customerId, recentOrders);
        }

        // Check 2: Daily spend
        OffsetDateTime oneDayAgo = OffsetDateTime.now().minusDays(1);
        Long dailySpend = orderRepository.sumTotalCentsByCustomerIdAndCreatedAtAfter(customerId, oneDayAgo);
        if (dailySpend == null) dailySpend = 0L;
        if (dailySpend + orderTotalCents > maxSpendPerDayCents) {
            warnings.add("Daily spend limit would be exceeded (₹" + ((dailySpend + orderTotalCents) / 100) + "/₹" + (maxSpendPerDayCents / 100) + ")");
            log.warn("FRAUD: Customer {} daily spend would exceed limit: {} + {} > {}",
                    customerId, dailySpend, orderTotalCents, maxSpendPerDayCents);
        }

        // Check 3: Cancelled orders in last 24h
        long cancelledToday = orderRepository.countByCustomerIdAndStatusAndCreatedAtAfter(
                customerId, OrderStatus.CANCELLED, oneDayAgo);
        if (cancelledToday >= maxCancelledPerDay) {
            warnings.add("Too many cancelled orders today (" + cancelledToday + "/" + maxCancelledPerDay + ")");
            blocked = true;
            log.warn("FRAUD: Customer {} exceeded max cancellations/day: {}", customerId, cancelledToday);
        }

        return new FraudCheckResult(blocked, warnings);
    }

    /**
     * Result of a fraud check.
     */
    public record FraudCheckResult(boolean blocked, List<String> warnings) {
        public static FraudCheckResult safe() {
            return new FraudCheckResult(false, List.of());
        }
    }
}
