package com.quickbite.audit.service;

import com.quickbite.audit.entity.AuditLog;
import com.quickbite.audit.repository.AuditLogRepository;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for writing audit log entries for admin actions.
 * Phase 1 — wires the existing AuditLog entity that was previously unused.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    /**
     * Record an admin action in the audit log.
     */
    @Transactional
    public void recordAction(UUID actorId, String action, String entity,
                             UUID entityId, Map<String, Object> oldValues,
                             Map<String, Object> newValues) {
        try {
            User actor = userRepository.findById(actorId).orElse(null);
            AuditLog entry = AuditLog.builder()
                    .user(actor)
                    .action(action)
                    .entity(entity)
                    .entityId(entityId)
                    .oldValues(oldValues)
                    .newValues(newValues)
                    .build();
            auditLogRepository.save(entry);
            log.debug("Audit: {} {} {} by user {}", action, entity, entityId, actorId);
        } catch (Exception e) {
            log.error("Failed to write audit log: {} {} {}", action, entity, entityId, e);
        }
    }
}
