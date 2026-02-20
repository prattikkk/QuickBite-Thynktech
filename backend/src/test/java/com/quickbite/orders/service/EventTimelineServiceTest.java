package com.quickbite.orders.service;

import com.quickbite.orders.entity.EventTimeline;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.EventTimelineRepository;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventTimelineService.
 */
@ExtendWith(MockitoExtension.class)
class EventTimelineServiceTest {

    @Mock
    private EventTimelineRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventTimelineService service;

    private UUID orderId;
    private UUID actorId;
    private User vendorUser;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        vendorUser = User.builder()
                .id(actorId)
                .name("Vendor User")
                .role(Role.builder().name("VENDOR").build())
                .build();
    }

    @Test
    void recordStatusChange_savesEntryWithResolvedRole() {
        // Arrange
        when(userRepository.findById(actorId)).thenReturn(Optional.of(vendorUser));
        when(repository.save(any(EventTimeline.class))).thenAnswer(inv -> {
            EventTimeline e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        Map<String, Object> meta = Map.of("note", "Test note");

        // Act
        EventTimeline result = service.recordStatusChange(
                orderId, actorId, OrderStatus.PLACED, OrderStatus.ACCEPTED, meta);

        // Assert
        assertThat(result).isNotNull();

        ArgumentCaptor<EventTimeline> captor = ArgumentCaptor.forClass(EventTimeline.class);
        verify(repository).save(captor.capture());

        EventTimeline saved = captor.getValue();
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getActorRole()).isEqualTo("VENDOR");
        assertThat(saved.getEventType()).isEqualTo("STATUS_CHANGE");
        assertThat(saved.getOldStatus()).isEqualTo("PLACED");
        assertThat(saved.getNewStatus()).isEqualTo("ACCEPTED");
        assertThat(saved.getMeta()).containsEntry("note", "Test note");
    }

    @Test
    void recordStatusChange_nullActor_usesSystemRole() {
        // Arrange
        when(repository.save(any(EventTimeline.class))).thenAnswer(inv -> {
            EventTimeline e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        // Act
        EventTimeline result = service.recordStatusChange(
                orderId, null, null, OrderStatus.PLACED, null);

        // Assert
        ArgumentCaptor<EventTimeline> captor = ArgumentCaptor.forClass(EventTimeline.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorRole()).isEqualTo("SYSTEM");
    }

    @Test
    void recordEvent_savesGenericEvent() {
        // Arrange
        when(userRepository.findById(actorId)).thenReturn(Optional.of(vendorUser));
        when(repository.save(any(EventTimeline.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> meta = Map.of("driverId", "abc-123");

        // Act
        EventTimeline result = service.recordEvent(orderId, actorId, "DRIVER_ASSIGNED", meta);

        // Assert
        ArgumentCaptor<EventTimeline> captor = ArgumentCaptor.forClass(EventTimeline.class);
        verify(repository).save(captor.capture());

        EventTimeline saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("DRIVER_ASSIGNED");
        assertThat(saved.getOldStatus()).isNull();
        assertThat(saved.getNewStatus()).isNull();
        assertThat(saved.getMeta()).containsEntry("driverId", "abc-123");
    }

    @Test
    void getTimeline_returnsOrderedEntries() {
        // Arrange
        EventTimeline entry1 = EventTimeline.builder().id(UUID.randomUUID()).orderId(orderId).build();
        EventTimeline entry2 = EventTimeline.builder().id(UUID.randomUUID()).orderId(orderId).build();
        when(repository.findByOrderIdOrderByCreatedAtAsc(orderId)).thenReturn(List.of(entry1, entry2));

        // Act
        List<EventTimeline> result = service.getTimeline(orderId);

        // Assert
        assertThat(result).hasSize(2);
        verify(repository).findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Test
    void recordStatusChange_unknownUser_usesUnknownRole() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
        when(repository.save(any(EventTimeline.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        service.recordStatusChange(orderId, unknownId, OrderStatus.PLACED, OrderStatus.CANCELLED, null);

        // Assert
        ArgumentCaptor<EventTimeline> captor = ArgumentCaptor.forClass(EventTimeline.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getActorRole()).isEqualTo("UNKNOWN");
    }
}
