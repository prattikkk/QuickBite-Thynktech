package com.quickbite.analytics.service;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.vendors.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing vendor-specific analytics over configurable time periods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorAnalyticsService {

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Compute analytics for a vendor over the requested period.
     *
     * @param vendorId the vendor UUID
     * @param period   "daily", "weekly", or "monthly"
     * @return map containing totalOrders, totalRevenueCents, averagePrepTime,
     *         cancellationRate, topItems, ordersByDay
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVendorAnalytics(UUID vendorId, String period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startDate = resolveStartDate(now, period);

        List<Order> orders = orderRepository.findVendorOrdersInDateRange(vendorId, startDate, now);

        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("vendorId", vendorId);
        analytics.put("period", period);
        analytics.put("startDate", startDate);
        analytics.put("endDate", now);

        // Total orders
        analytics.put("totalOrders", orders.size());

        // Total revenue (only DELIVERED orders)
        long totalRevenueCents = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToLong(Order::getTotalCents)
                .sum();
        analytics.put("totalRevenueCents", totalRevenueCents);

        // Average prep time
        OptionalDouble avgPrep = orders.stream()
                .filter(o -> o.getEstimatedPrepMins() != null)
                .mapToInt(Order::getEstimatedPrepMins)
                .average();
        analytics.put("averagePrepTime", avgPrep.isPresent() ? Math.round(avgPrep.getAsDouble() * 10.0) / 10.0 : null);

        // Cancellation rate
        long cancelledCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();
        double cancellationRate = orders.isEmpty() ? 0.0 : (double) cancelledCount / orders.size();
        analytics.put("cancellationRate", Math.round(cancellationRate * 10000.0) / 10000.0);

        // Top items
        analytics.put("topItems", computeTopItems(orders));

        // Orders by day
        analytics.put("ordersByDay", computeOrdersByDay(orders));

        return analytics;
    }

    /**
     * Export analytics as CSV bytes.
     */
    @Transactional(readOnly = true)
    public byte[] exportAnalyticsCsv(UUID vendorId, String period) {
        Map<String, Object> analytics = getVendorAnalytics(vendorId, period);

        StringBuilder sb = new StringBuilder();
        sb.append("metric,value\n");
        sb.append("vendorId,").append(vendorId).append('\n');
        sb.append("period,").append(period).append('\n');
        sb.append("totalOrders,").append(analytics.get("totalOrders")).append('\n');
        sb.append("totalRevenueCents,").append(analytics.get("totalRevenueCents")).append('\n');
        sb.append("averagePrepTime,").append(analytics.get("averagePrepTime")).append('\n');
        sb.append("cancellationRate,").append(analytics.get("cancellationRate")).append('\n');

        sb.append('\n');
        sb.append("date,orderCount,revenueCents\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> byDay = (List<Map<String, Object>>) analytics.get("ordersByDay");
        if (byDay != null) {
            for (Map<String, Object> row : byDay) {
                sb.append(row.get("date")).append(',')
                  .append(row.get("count")).append(',')
                  .append(row.get("revenue")).append('\n');
            }
        }

        sb.append('\n');
        sb.append("itemName,orderCount,revenueCents\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> topItems = (List<Map<String, Object>>) analytics.get("topItems");
        if (topItems != null) {
            for (Map<String, Object> item : topItems) {
                sb.append(escapeCsv(String.valueOf(item.get("name")))).append(',')
                  .append(item.get("count")).append(',')
                  .append(item.get("revenue")).append('\n');
            }
        }

        return sb.toString().getBytes();
    }

    // ---- private helpers ----

    private OffsetDateTime resolveStartDate(OffsetDateTime now, String period) {
        return switch (period != null ? period.toLowerCase() : "weekly") {
            case "daily" -> now.truncatedTo(ChronoUnit.DAYS);
            case "monthly" -> now.minusDays(30).truncatedTo(ChronoUnit.DAYS);
            default -> now.minusDays(7).truncatedTo(ChronoUnit.DAYS);
        };
    }

    private List<Map<String, Object>> computeTopItems(List<Order> orders) {
        Map<String, long[]> itemAggregates = new LinkedHashMap<>(); // name -> [count, revenue]

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.CANCELLED) continue;
            if (order.getItems() == null) continue;
            order.getItems().forEach(item -> {
                String name = (item.getMenuItem() != null && item.getMenuItem().getName() != null)
                        ? item.getMenuItem().getName() : "Unknown";
                long[] agg = itemAggregates.computeIfAbsent(name, k -> new long[2]);
                agg[0] += item.getQuantity();
                agg[1] += item.calculateTotal() != null ? item.calculateTotal() : 0L;
            });
        }

        return itemAggregates.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue()[0]);
                    m.put("revenue", e.getValue()[1]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> computeOrdersByDay(List<Order> orders) {
        Map<LocalDate, long[]> dayAggregates = new TreeMap<>();

        for (Order order : orders) {
            LocalDate date = order.getCreatedAt().toLocalDate();
            long[] agg = dayAggregates.computeIfAbsent(date, k -> new long[2]);
            agg[0]++;
            if (order.getStatus() == OrderStatus.DELIVERED) {
                agg[1] += order.getTotalCents();
            }
        }

        return dayAggregates.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey().toString());
                    m.put("count", e.getValue()[0]);
                    m.put("revenue", e.getValue()[1]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
