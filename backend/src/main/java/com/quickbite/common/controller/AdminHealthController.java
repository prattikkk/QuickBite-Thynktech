package com.quickbite.common.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.common.feature.FeatureFlagService;
import com.quickbite.orders.repository.EventTimelineRepository;
import com.quickbite.payments.repository.WebhookDlqRepository;
import com.quickbite.payments.repository.WebhookEventRepository;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin-only health summary endpoint.
 * Returns operational metrics: DB pool stats, webhook queue depths,
 * DLQ count, recent metrics, and feature flags.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/health-summary")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminHealthController {

    private final DataSource dataSource;
    private final WebhookEventRepository webhookEventRepository;
    private final WebhookDlqRepository webhookDlqRepository;
    private final EventTimelineRepository eventTimelineRepository;
    private final MeterRegistry meterRegistry;
    private final FeatureFlagService featureFlagService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealthSummary() {
        Map<String, Object> summary = new HashMap<>();

        // 1. DB connection pool stats
        summary.put("dbPool", getDbPoolStats());

        // 2. Webhook queue
        Map<String, Object> webhookStats = new HashMap<>();
        webhookStats.put("pendingCount", webhookEventRepository.countByProcessedFalse());
        webhookStats.put("dlqCount", webhookDlqRepository.count());
        summary.put("webhooks", webhookStats);

        // 3. Business metrics from Micrometer
        summary.put("metrics", getBusinessMetrics());

        // 4. Feature flags
        summary.put("featureFlags", featureFlagService.getAllFlags());

        // 5. Timeline event count (total auditable events)
        summary.put("totalTimelineEvents", eventTimelineRepository.count());

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    private Map<String, Object> getDbPoolStats() {
        Map<String, Object> pool = new HashMap<>();
        try {
            if (dataSource instanceof HikariDataSource hikari) {
                HikariPoolMXBean mxBean = hikari.getHikariPoolMXBean();
                if (mxBean != null) {
                    pool.put("activeConnections", mxBean.getActiveConnections());
                    pool.put("idleConnections", mxBean.getIdleConnections());
                    pool.put("totalConnections", mxBean.getTotalConnections());
                    pool.put("threadsAwaitingConnection", mxBean.getThreadsAwaitingConnection());
                } else {
                    pool.put("status", "pool-not-initialized");
                }
            } else {
                pool.put("status", "non-hikari-datasource");
            }
        } catch (Exception e) {
            pool.put("error", e.getMessage());
        }
        return pool;
    }

    private Map<String, Object> getBusinessMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("ordersCreated", getCounterValue("orders.created"));
        metrics.put("orderTransitions", getCounterValue("orders.transitions"));
        metrics.put("paymentIntentsCreated", getCounterValue("payments.intent.created"));
        metrics.put("paymentSuccess", getCounterValue("payments.success"));
        metrics.put("paymentFailed", getCounterValue("payments.failed"));
        metrics.put("webhooksProcessed", getCounterValue("webhooks.processed"));
        metrics.put("webhooksFailed", getCounterValue("webhooks.failed"));
        metrics.put("webhooksDlq", getCounterValue("webhooks.dlq"));
        return metrics;
    }

    private double getCounterValue(String name) {
        Counter counter = meterRegistry.find(name).counter();
        return counter != null ? counter.count() : 0.0;
    }
}
