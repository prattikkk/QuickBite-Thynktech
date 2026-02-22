package com.quickbite.orders.driver;

import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverLocationService.
 * Phase 2 — Foreground Live Location.
 */
@ExtendWith(MockitoExtension.class)
class DriverLocationServiceTest {

    @Mock private DriverLocationRepository locationRepository;
    @Mock private DriverProfileService profileService;
    @Mock private UserRepository userRepository;

    @InjectMocks private DriverLocationService service;

    private UUID driverId;
    private User driver;

    @BeforeEach
    void setUp() {
        driverId = UUID.randomUUID();
        driver = User.builder()
                .id(driverId)
                .email("driver@test.com")
                .name("Test Driver")
                .role(Role.builder().name("DRIVER").build())
                .active(true)
                .build();
    }

    // ── recordLocation ───────────────────────────────────────────

    @Test
    void recordLocation_savesAndUpdatesProfile() {
        when(locationRepository.countByDriverIdSince(eq(driverId), any())).thenReturn(0L);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(locationRepository.save(any())).thenAnswer(inv -> {
            DriverLocation loc = inv.getArgument(0);
            loc.setId(UUID.randomUUID());
            return loc;
        });

        DriverLocation result = service.recordLocation(driverId, 40.7128, -74.0060, 5.0, 10.0, 180.0);

        assertThat(result).isNotNull();
        assertThat(result.getLat().doubleValue()).isCloseTo(40.7128, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.getLng().doubleValue()).isCloseTo(-74.006, org.assertj.core.data.Offset.offset(0.001));
        assertThat(result.getAccuracy()).isEqualTo(5.0);
        assertThat(result.getSpeed()).isEqualTo(10.0);
        assertThat(result.getHeading()).isEqualTo(180.0);

        verify(locationRepository).save(any());
        verify(profileService).updateLocation(driverId, 40.7128, -74.0060);
    }

    @Test
    void recordLocation_rateLimited_returnsNull() {
        // 12 samples already recorded in the last minute
        when(locationRepository.countByDriverIdSince(eq(driverId), any())).thenReturn(12L);

        DriverLocation result = service.recordLocation(driverId, 40.7128, -74.0060, null, null, null);

        assertThat(result).isNull();
        verify(locationRepository, never()).save(any());
        verify(profileService, never()).updateLocation(any(), anyDouble(), anyDouble());
    }

    @Test
    void recordLocation_userNotFound_throws() {
        when(locationRepository.countByDriverIdSince(eq(driverId), any())).thenReturn(0L);
        when(userRepository.findById(driverId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordLocation(driverId, 40.0, -74.0, null, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Driver user not found");
    }

    @Test
    void recordLocation_nullOptionalFields_savesSuccessfully() {
        when(locationRepository.countByDriverIdSince(eq(driverId), any())).thenReturn(0L);
        when(userRepository.findById(driverId)).thenReturn(Optional.of(driver));
        when(locationRepository.save(any())).thenAnswer(inv -> {
            DriverLocation loc = inv.getArgument(0);
            loc.setId(UUID.randomUUID());
            return loc;
        });

        DriverLocation result = service.recordLocation(driverId, 40.7128, -74.0060, null, null, null);

        assertThat(result).isNotNull();
        assertThat(result.getAccuracy()).isNull();
        assertThat(result.getSpeed()).isNull();
        assertThat(result.getHeading()).isNull();
    }

    // ── getRecentLocations ───────────────────────────────────────

    @Test
    void getRecentLocations_returnsMappedDTOs() {
        DriverLocation loc1 = DriverLocation.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .lat(BigDecimal.valueOf(40.7128))
                .lng(BigDecimal.valueOf(-74.0060))
                .accuracy(5.0)
                .speed(10.0)
                .heading(90.0)
                .recordedAt(OffsetDateTime.now().minusMinutes(1))
                .build();

        DriverLocation loc2 = DriverLocation.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .lat(BigDecimal.valueOf(40.7130))
                .lng(BigDecimal.valueOf(-74.0065))
                .accuracy(3.0)
                .speed(15.0)
                .heading(270.0)
                .recordedAt(OffsetDateTime.now())
                .build();

        when(locationRepository.findTop20ByDriverIdOrderByRecordedAtDesc(driverId))
                .thenReturn(List.of(loc2, loc1));

        List<DriverLocationDTO> result = service.getRecentLocations(driverId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDriverId()).isEqualTo(driverId);
        assertThat(result.get(0).getLat()).isCloseTo(40.713, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void getRecentLocations_empty_returnsEmpty() {
        when(locationRepository.findTop20ByDriverIdOrderByRecordedAtDesc(driverId))
                .thenReturn(List.of());

        List<DriverLocationDTO> result = service.getRecentLocations(driverId);

        assertThat(result).isEmpty();
    }

    // ── getLastLocation ──────────────────────────────────────────

    @Test
    void getLastLocation_returnsDTO() {
        DriverLocation loc = DriverLocation.builder()
                .id(UUID.randomUUID())
                .driver(driver)
                .lat(BigDecimal.valueOf(40.7128))
                .lng(BigDecimal.valueOf(-74.0060))
                .accuracy(5.0)
                .recordedAt(OffsetDateTime.now())
                .build();

        when(locationRepository.findFirstByDriverIdOrderByRecordedAtDesc(driverId)).thenReturn(loc);

        DriverLocationDTO result = service.getLastLocation(driverId);

        assertThat(result).isNotNull();
        assertThat(result.getDriverId()).isEqualTo(driverId);
    }

    @Test
    void getLastLocation_noLocations_returnsNull() {
        when(locationRepository.findFirstByDriverIdOrderByRecordedAtDesc(driverId)).thenReturn(null);

        DriverLocationDTO result = service.getLastLocation(driverId);

        assertThat(result).isNull();
    }

    // ── startShift / endShift ────────────────────────────────────

    @Test
    void startShift_delegatesToProfileService() {
        DriverProfileDTO expected = DriverProfileDTO.builder()
                .userId(driverId)
                .isOnline(true)
                .shiftStartedAt(OffsetDateTime.now())
                .build();
        when(profileService.startShift(driverId)).thenReturn(expected);

        DriverProfileDTO result = service.startShift(driverId);

        assertThat(result.getIsOnline()).isTrue();
        assertThat(result.getShiftStartedAt()).isNotNull();
        verify(profileService).startShift(driverId);
    }

    @Test
    void endShift_delegatesToProfileService() {
        DriverProfileDTO expected = DriverProfileDTO.builder()
                .userId(driverId)
                .isOnline(false)
                .shiftEndedAt(OffsetDateTime.now())
                .build();
        when(profileService.endShift(driverId)).thenReturn(expected);

        DriverProfileDTO result = service.endShift(driverId);

        assertThat(result.getIsOnline()).isFalse();
        assertThat(result.getShiftEndedAt()).isNotNull();
        verify(profileService).endShift(driverId);
    }

    // ── pruneOldLocations ────────────────────────────────────────

    @Test
    void pruneOldLocations_deletesOldRecords() {
        when(locationRepository.deleteOlderThan(any())).thenReturn(42);

        service.pruneOldLocations();

        verify(locationRepository).deleteOlderThan(any());
    }

    @Test
    void pruneOldLocations_nothingToDelete_stillCalls() {
        when(locationRepository.deleteOlderThan(any())).thenReturn(0);

        service.pruneOldLocations();

        verify(locationRepository).deleteOlderThan(any());
    }
}
