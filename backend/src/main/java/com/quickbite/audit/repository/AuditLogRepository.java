package com.quickbite.audit.repository;

import com.quickbite.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for AuditLog entity operations.
 * Provides audit trail queries for security and compliance.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    /**
     * Find audit logs by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find audit logs by action.
     *
     * @param action the action name
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs for a specific entity.
     *
     * @param entity the entity type
     * @param entityId the entity ID
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByEntityAndEntityId(String entity, UUID entityId, Pageable pageable);

    /**
     * Find audit logs in date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination information
     * @return Page of audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    Page<AuditLog> findLogsInDateRange(
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate,
        Pageable pageable
    );

    /**
     * Find audit logs by user and action.
     *
     * @param userId the user ID
     * @param action the action name
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByUserIdAndAction(UUID userId, String action, Pageable pageable);

    /**
     * Find recent login attempts.
     *
     * @param action the login action (LOGIN or LOGIN_FAILED)
     * @param limit maximum number of results
     * @return List of recent login logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentLoginAttempts(@Param("actions") List<String> actions, Pageable pageable);

    /**
     * Find failed login attempts for a user in time window.
     *
     * @param userId the user ID (nullable for email-based checks)
     * @param since time threshold
     * @return count of failed attempts
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.user.id = :userId " +
           "AND a.action = 'LOGIN_FAILED' AND a.createdAt > :since")
    Long countFailedLoginAttempts(@Param("userId") UUID userId, @Param("since") OffsetDateTime since);

    /**
     * Find audit logs by IP address (for security monitoring).
     *
     * @param ipAddress the IP address
     * @param pageable pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);

    /**
     * Delete old audit logs (for data retention policy).
     *
     * @param beforeDate delete logs before this date
     */
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :beforeDate")
    void deleteOldLogs(@Param("beforeDate") OffsetDateTime beforeDate);
}
