package com.quickbite.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Scheduled service for data retention policy enforcement.
 * Periodically purges stale data from high-growth tables.
 * Retention periods are configurable via application.properties.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Value("${retention.enabled:true}")
    private boolean retentionEnabled;

    @Value("${retention.audit-log-days:90}")
    private int auditLogRetentionDays;

    @Value("${retention.driver-location-days:30}")
    private int driverLocationRetentionDays;

    @Value("${retention.webhook-events-days:60}")
    private int webhookEventsRetentionDays;

    @Value("${retention.notification-days:60}")
    private int notificationRetentionDays;

    /**
     * Run data retention daily at 3 AM (server time).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void enforceRetention() {
        if (!retentionEnabled) {
            log.debug("Data retention is disabled");
            return;
        }

        log.info("Starting data retention enforcement...");

        // 1. Purge old audit log entries
        purgeTable("audit_logs", "created_at", auditLogRetentionDays);

        // 2. Purge old driver location samples
        purgeTable("driver_locations", "recorded_at", driverLocationRetentionDays);

        // 3. Purge processed webhook events
        purgeProcessedWebhooks(webhookEventsRetentionDays);

        // 4. Purge old read notifications
        purgeReadNotifications(notificationRetentionDays);

        // 5. Purge old delivery status history (keep last 60 days)
        purgeTable("delivery_statuses", "changed_at", 60);

        // 6. Purge old event timeline entries
        purgeTable("event_timeline", "created_at", 90);

        // 7. Purge expired idempotency keys
        purgeTable("idempotency_keys", "expires_at", 0);

        log.info("Data retention enforcement complete");
    }

    private void purgeTable(String tableName, String dateColumn, int retentionDays) {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
            String sql = String.format("DELETE FROM %s WHERE %s < ?", tableName, dateColumn);
            int deleted = jdbcTemplate.update(sql, cutoff);
            if (deleted > 0) {
                logRetention(tableName, deleted, retentionDays);
                log.info("Purged {} records from {} (older than {} days)", deleted, tableName, retentionDays);
            }
        } catch (Exception e) {
            log.warn("Retention purge failed for {}: {}", tableName, e.getMessage());
        }
    }

    private void purgeProcessedWebhooks(int retentionDays) {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
            int deleted = jdbcTemplate.update(
                    "DELETE FROM webhook_events WHERE processed = TRUE AND created_at < ?", cutoff);
            if (deleted > 0) {
                logRetention("webhook_events", deleted, retentionDays);
                log.info("Purged {} processed webhook events (older than {} days)", deleted, retentionDays);
            }
        } catch (Exception e) {
            log.warn("Webhook event retention purge failed: {}", e.getMessage());
        }
    }

    private void purgeReadNotifications(int retentionDays) {
        try {
            OffsetDateTime cutoff = OffsetDateTime.now().minusDays(retentionDays);
            int deleted = jdbcTemplate.update(
                    "DELETE FROM notifications WHERE read = TRUE AND created_at < ?", cutoff);
            if (deleted > 0) {
                logRetention("notifications", deleted, retentionDays);
                log.info("Purged {} read notifications (older than {} days)", deleted, retentionDays);
            }
        } catch (Exception e) {
            log.warn("Notification retention purge failed: {}", e.getMessage());
        }
    }

    private void logRetention(String tableName, int recordsDeleted, int retentionDays) {
        try {
            jdbcTemplate.update(
                    "INSERT INTO data_retention_log (table_name, records_deleted, retention_days) VALUES (?, ?, ?)",
                    tableName, recordsDeleted, retentionDays);
        } catch (Exception e) {
            log.debug("Failed to log retention entry for {}: {}", tableName, e.getMessage());
        }
    }
}
