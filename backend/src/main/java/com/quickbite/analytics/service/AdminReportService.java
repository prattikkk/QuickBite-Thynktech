package com.quickbite.analytics.service;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing platform-wide admin reports and KPIs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    /**
     * Get platform KPIs for the requested period.
     *
     * @param period "daily", "weekly", or "monthly"
     * @return map with totalOrders, totalRevenueCents, totalCustomers, totalVendors,
     *         totalDrivers, averageDeliveryTime, repeatOrderRate
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformKpis(String period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startDate = resolveStartDate(now, period);

        var ordersPage = orderRepository.findOrdersInDateRange(startDate, now, PageRequest.of(0, Integer.MAX_VALUE));
        List<Order> orders = ordersPage.getContent();

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("period", period);
        kpis.put("startDate", startDate);
        kpis.put("endDate", now);
        kpis.put("totalOrders", orders.size());

        long totalRevenueCents = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToLong(Order::getTotalCents)
                .sum();
        kpis.put("totalRevenueCents", totalRevenueCents);

        kpis.put("totalCustomers", userRepository.count());
        kpis.put("totalVendors", vendorRepository.count());
        
        // Count users with DRIVER role
        long totalDrivers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "DRIVER".equals(u.getRole().getName()))
                .count();
        kpis.put("totalDrivers", totalDrivers);

        // Average delivery time in minutes (for delivered orders)
        OptionalDouble avgDelivery = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getDeliveredAt() != null)
                .mapToLong(o -> Duration.between(o.getCreatedAt(), o.getDeliveredAt()).toMinutes())
                .average();
        kpis.put("averageDeliveryTime", avgDelivery.isPresent() ? Math.round(avgDelivery.getAsDouble() * 10.0) / 10.0 : null);

        // Repeat order rate: customers with >1 order / total customers in period
        Map<UUID, Long> customerOrderCounts = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCustomer().getId(), Collectors.counting()));
        long customersWithOrders = customerOrderCounts.size();
        long repeatCustomers = customerOrderCounts.values().stream().filter(c -> c > 1).count();
        double repeatOrderRate = customersWithOrders == 0 ? 0.0 : (double) repeatCustomers / customersWithOrders;
        kpis.put("repeatOrderRate", Math.round(repeatOrderRate * 10000.0) / 10000.0);

        return kpis;
    }

    /**
     * Get revenue breakdown in daily/weekly buckets over the requested period.
     *
     * @param period "daily", "weekly", or "monthly"
     * @return list of maps with date, orderCount, revenueCents
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRevenueReport(String period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startDate = resolveStartDate(now, period);

        var ordersPage = orderRepository.findOrdersInDateRange(startDate, now, PageRequest.of(0, Integer.MAX_VALUE));
        List<Order> orders = ordersPage.getContent();

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
                    m.put("orderCount", e.getValue()[0]);
                    m.put("revenueCents", e.getValue()[1]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Get delivery time statistics: avg, min, max, p50, p90, p95.
     *
     * @return map with delivery time stats in minutes
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDeliveryTimeReport() {
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        var ordersPage = orderRepository.findOrdersInDateRange(startDate, now, PageRequest.of(0, Integer.MAX_VALUE));
        List<Long> deliveryMinutes = ordersPage.getContent().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getDeliveredAt() != null)
                .map(o -> Duration.between(o.getCreatedAt(), o.getDeliveredAt()).toMinutes())
                .sorted()
                .collect(Collectors.toList());

        Map<String, Object> report = new LinkedHashMap<>();
        if (deliveryMinutes.isEmpty()) {
            report.put("avg", null);
            report.put("min", null);
            report.put("max", null);
            report.put("p50", null);
            report.put("p90", null);
            report.put("p95", null);
            report.put("sampleSize", 0);
            return report;
        }

        double avg = deliveryMinutes.stream().mapToLong(Long::longValue).average().orElse(0);
        report.put("avg", Math.round(avg * 10.0) / 10.0);
        report.put("min", deliveryMinutes.get(0));
        report.put("max", deliveryMinutes.get(deliveryMinutes.size() - 1));
        report.put("p50", percentile(deliveryMinutes, 50));
        report.put("p90", percentile(deliveryMinutes, 90));
        report.put("p95", percentile(deliveryMinutes, 95));
        report.put("sampleSize", deliveryMinutes.size());

        return report;
    }

    /**
     * Export a report as CSV bytes.
     *
     * @param type   "revenue" or "kpis"
     * @param period "daily", "weekly", or "monthly"
     * @return CSV content as byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportReportCsv(String type, String period) {
        StringBuilder sb = new StringBuilder();

        if ("revenue".equalsIgnoreCase(type)) {
            List<Map<String, Object>> revenue = getRevenueReport(period);
            sb.append("date,orderCount,revenueCents\n");
            for (Map<String, Object> row : revenue) {
                sb.append(row.get("date")).append(',')
                  .append(row.get("orderCount")).append(',')
                  .append(row.get("revenueCents")).append('\n');
            }
        } else {
            Map<String, Object> kpis = getPlatformKpis(period);
            sb.append("metric,value\n");
            for (Map.Entry<String, Object> entry : kpis.entrySet()) {
                sb.append(entry.getKey()).append(',').append(entry.getValue()).append('\n');
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

    private long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int index = (int) Math.ceil(p / 100.0 * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }
}
