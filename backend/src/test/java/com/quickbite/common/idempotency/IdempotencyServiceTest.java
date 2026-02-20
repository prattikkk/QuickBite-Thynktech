package com.quickbite.common.idempotency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IdempotencyService.
 */
@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private IdempotencyKeyRepository repository;

    @InjectMocks
    private IdempotencyService service;

    @Test
    void findExisting_returnsKeyWhenFound() {
        // Arrange
        UUID userId = UUID.randomUUID();
        IdempotencyKey existing = IdempotencyKey.builder()
                .key("test-key")
                .userId(userId)
                .endpoint("/api/orders")
                .used(true)
                .responseStatus(201)
                .responseBody("{\"id\":\"abc\"}")
                .build();

        when(repository.findValidKey(eq("test-key"), eq(userId), eq("/api/orders"), any(OffsetDateTime.class)))
                .thenReturn(Optional.of(existing));

        // Act
        Optional<IdempotencyKey> result = service.findExisting("test-key", userId, "/api/orders");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getResponseBody()).isEqualTo("{\"id\":\"abc\"}");
    }

    @Test
    void findExisting_returnsEmptyWhenNotFound() {
        UUID userId = UUID.randomUUID();
        when(repository.findValidKey(any(), any(), any(), any())).thenReturn(Optional.empty());

        Optional<IdempotencyKey> result = service.findExisting("new-key", userId, "/api/orders");

        assertThat(result).isEmpty();
    }

    @Test
    void store_savesKeyWithExpiry() {
        UUID userId = UUID.randomUUID();
        when(repository.save(any(IdempotencyKey.class))).thenAnswer(inv -> {
            IdempotencyKey k = inv.getArgument(0);
            k.setId(UUID.randomUUID());
            return k;
        });

        // Act
        IdempotencyKey result = service.store(
                "key-1", userId, "/api/orders", "hash123", 201, "{\"id\":\"xyz\"}");

        // Assert
        ArgumentCaptor<IdempotencyKey> captor = ArgumentCaptor.forClass(IdempotencyKey.class);
        verify(repository).save(captor.capture());

        IdempotencyKey saved = captor.getValue();
        assertThat(saved.getKey()).isEqualTo("key-1");
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEndpoint()).isEqualTo("/api/orders");
        assertThat(saved.getRequestHash()).isEqualTo("hash123");
        assertThat(saved.getResponseStatus()).isEqualTo(201);
        assertThat(saved.getResponseBody()).isEqualTo("{\"id\":\"xyz\"}");
        assertThat(saved.getUsed()).isTrue();
        assertThat(saved.getExpiresAt()).isAfter(OffsetDateTime.now().plusHours(23));
    }

    @Test
    void purgeExpired_callsRepositoryDelete() {
        when(repository.deleteExpiredKeys(any(OffsetDateTime.class))).thenReturn(5);

        // Act
        service.purgeExpired();

        // Assert
        verify(repository).deleteExpiredKeys(any(OffsetDateTime.class));
    }

    @Test
    void purgeExpired_noExpiredKeys_stillCallsRepository() {
        when(repository.deleteExpiredKeys(any(OffsetDateTime.class))).thenReturn(0);

        service.purgeExpired();

        verify(repository).deleteExpiredKeys(any(OffsetDateTime.class));
    }
}
