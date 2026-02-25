package com.quickbite.analytics.service;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.vendors.entity.VendorCommission;
import com.quickbite.vendors.repository.VendorCommissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for vendor-specific analytics and reporting.
 * Provides revenue, order stats, and commission breakdown.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorReportService {

    private final OrderRepository orderRepository;
    private final VendorCommissionRepository vendorCommissionRepository;

    /**
     * Get vendor-specific KPIs for a period.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getVendorKpis(UUID vendorId, String period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startDate = resolveStartDate(now, period);

        List<Order> orders = orderRepository.findVendorOrdersInDateRange(vendorId, startDate, now);

        Map<String, Object> kpis = new LinkedHashMap<>();
        kpis.put("vendorId", vendorId);
        kpis.put("period", period);
        kpis.put("startDate", startDate);
        kpis.put("endDate", now);
        kpis.put("totalOrders", orders.size());

        long deliveredCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();
        kpis.put("deliveredOrders", deliveredCount);

        long cancelledCount = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .count();
        kpis.put("cancelledOrders", cancelledCount);

        long totalRevenueCents = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToLong(Order::getTotalCents)
                .sum();
        kpis.put("totalRevenueCents", totalRevenueCents);

        long totalCommissionCents = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getCommissionCents() != null)
                .mapToLong(Order::getCommissionCents)
                .sum();
        kpis.put("totalCommissionCents", totalCommissionCents);

        long totalPayoutCents = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED && o.getVendorPayoutCents() != null)
                .mapToLong(Order::getVendorPayoutCents)
                .sum();
        kpis.put("vendorPayoutCents", totalPayoutCents);

        OptionalDouble avgOrderValue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .mapToLong(Order::getTotalCents)
                .average();
        kpis.put("avgOrderValueCents", avgOrderValue.isPresent() ? Math.round(avgOrderValue.getAsDouble()) : 0);

        OptionalDouble avgPrepTime = orders.stream()
                .filter(o -> o.getEstimatedPrepMins() != null)
                .mapToInt(Order::getEstimatedPrepMins)
                .average();
        kpis.put("avgPrepTimeMins", avgPrepTime.isPresent() ? Math.round(avgPrepTime.getAsDouble()) : null);

        // Acceptance rate (not cancelled / total)
        double acceptanceRate = orders.isEmpty() ? 1.0
                : (double) (orders.size() - cancelledCount) / orders.size();
        kpis.put("acceptanceRate", Math.round(acceptanceRate * 10000.0) / 10000.0);

        // Commission info
        vendorCommissionRepository.findActiveByVendorId(vendorId).ifPresent(vc -> {
            kpis.put("commissionRateBps", vc.getCommissionRateBps());
            kpis.put("flatFeeCents", vc.getFlatFeeCents());
        });

        return kpis;
    }

    /**
     * Get daily revenue breakdown for a vendor.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getVendorRevenueBreakdown(UUID vendorId, String period) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime startDate = resolveStartDate(now, period);

        List<Order> orders = orderRepository.findVendorOrdersInDateRange(vendorId, startDate, now);

        Map<String, long[]> dayAggregates = new TreeMap<>();
        for (Order order : orders) {
            String date = order.getCreatedAt().toLocalDate().toString();
            long[] agg = dayAggregates.computeIfAbsent(date, k -> new long[4]); // [orders, revenue, commission, payout]
            agg[0]++;
            if (order.getStatus() == OrderStatus.DELIVERED) {
                agg[1] += order.getTotalCents();
                agg[2] += order.getCommissionCents() != null ? order.getCommissionCents() : 0;
                agg[3] += order.getVendorPayoutCents() != null ? order.getVendorPayoutCents() : 0;
            }
        }

        return dayAggregates.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("date", e.getKey());
                    m.put("orderCount", e.getValue()[0]);
                    m.put("revenueCents", e.getValue()[1]);
                    m.put("commissionCents", e.getValue()[2]);
                    m.put("payoutCents", e.getValue()[3]);
                    return m;
                })
                .collect(Collectors.toList());
    }

    /**
     * Export vendor report as CSV.
     */
    @Transactional(readOnly = true)
    public byte[] exportVendorCsv(UUID vendorId, String period) {
        List<Map<String, Object>> breakdown = getVendorRevenueBreakdown(vendorId, period);
        StringBuilder sb = new StringBuilder();
        sb.append("date,orderCount,revenueCents,commissionCents,payoutCents\n");
        for (Map<String, Object> row : breakdown) {
            sb.append(row.get("date")).append(',')
              .append(row.get("orderCount")).append(',')
              .append(row.get("revenueCents")).append(',')
              .append(row.get("commissionCents")).append(',')
              .append(row.get("payoutCents")).append('\n');
        }
        return sb.toString().getBytes();
    }

    private OffsetDateTime resolveStartDate(OffsetDateTime now, String period) {
        return switch (period != null ? period.toLowerCase() : "weekly") {
            case "daily" -> now.truncatedTo(ChronoUnit.DAYS);
            case "monthly" -> now.minusDays(30).truncatedTo(ChronoUnit.DAYS);
            default -> now.minusDays(7).truncatedTo(ChronoUnit.DAYS);
        };
    }
}
