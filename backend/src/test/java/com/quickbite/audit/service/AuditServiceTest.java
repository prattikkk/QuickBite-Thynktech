package com.quickbite.audit.service;

import com.quickbite.audit.entity.AuditLog;
import com.quickbite.audit.repository.AuditLogRepository;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AuditService auditService;

    private UUID actorId;
    private User actor;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        actor = User.builder()
                .id(actorId)
                .email("admin@test.com")
                .name("Admin")
                .build();
    }

    @Test
    void recordAction_savesAuditLog() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID entityId = UUID.randomUUID();
        Map<String, Object> oldValues = Map.of("active", true);
        Map<String, Object> newValues = Map.of("active", false);

        auditService.recordAction(actorId, "BAN_USER", "User", entityId, oldValues, newValues);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(actor);
        assertThat(saved.getAction()).isEqualTo("BAN_USER");
        assertThat(saved.getEntity()).isEqualTo("User");
        assertThat(saved.getEntityId()).isEqualTo(entityId);
        assertThat(saved.getOldValues()).containsEntry("active", true);
        assertThat(saved.getNewValues()).containsEntry("active", false);
    }

    @Test
    void recordAction_actorNotFound_stillLogsWithNull() {
        when(userRepository.findById(actorId)).thenReturn(Optional.empty());
        when(auditLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID entityId = UUID.randomUUID();
        auditService.recordAction(actorId, "APPROVE_VENDOR", "Vendor", entityId, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isNull();
    }

    @Test
    void recordAction_exceptionInSave_doesNotThrow() {
        when(userRepository.findById(actorId)).thenReturn(Optional.of(actor));
        when(auditLogRepository.save(any())).thenThrow(new RuntimeException("DB error"));

        // Should not throw — silently logs the error
        auditService.recordAction(actorId, "TEST", "Entity", UUID.randomUUID(), null, null);

        verify(auditLogRepository).save(any());
    }
}
