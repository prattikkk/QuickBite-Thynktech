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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DriverProfileService.
 */
@ExtendWith(MockitoExtension.class)
class DriverProfileServiceTest {

    @Mock private DriverProfileRepository driverProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private DriverProfileService service;

    private UUID userId;
    private User user;
    private DriverProfile existingProfile;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder()
                .id(userId)
                .email("driver@test.com")
                .name("Test Driver")
                .role(Role.builder().name("DRIVER").build())
                .active(true)
                .build();

        existingProfile = DriverProfile.builder()
                .id(UUID.randomUUID())
                .user(user)
                .vehicleType("MOTORCYCLE")
                .isOnline(false)
                .totalDeliveries(0)
                .successRate(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void getOrCreateProfile_existingProfile_returns() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        DriverProfile result = service.getOrCreateProfile(userId);

        assertThat(result).isSameAs(existingProfile);
        verify(driverProfileRepository, never()).save(any());
    }

    @Test
    void getOrCreateProfile_noProfile_autoCreates() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverProfile result = service.getOrCreateProfile(userId);

        assertThat(result.getUser()).isSameAs(user);
        assertThat(result.getVehicleType()).isEqualTo("MOTORCYCLE");
        assertThat(result.getIsOnline()).isFalse();
        verify(driverProfileRepository).save(any());
    }

    @Test
    void getOrCreateProfile_userNotFound_throws() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrCreateProfile(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getProfileDTO_mapsFieldsCorrectly() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));

        DriverProfileDTO dto = service.getProfileDTO(userId);

        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.getName()).isEqualTo("Test Driver");
        assertThat(dto.getEmail()).isEqualTo("driver@test.com");
        assertThat(dto.getVehicleType()).isEqualTo("MOTORCYCLE");
        assertThat(dto.getIsOnline()).isFalse();
        assertThat(dto.getTotalDeliveries()).isEqualTo(0);
    }

    @Test
    void updateProfile_setsVehicleAndPlate() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDTO dto = service.updateProfile(userId, "CAR", "XY-5678");

        assertThat(dto.getVehicleType()).isEqualTo("CAR");
        assertThat(dto.getLicensePlate()).isEqualTo("XY-5678");
        verify(driverProfileRepository).save(existingProfile);
    }

    @Test
    void updateProfile_nullFields_noOverwrite() {
        existingProfile.setVehicleType("BIKE");
        existingProfile.setLicensePlate("OLD-001");
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDTO dto = service.updateProfile(userId, null, null);

        assertThat(dto.getVehicleType()).isEqualTo("BIKE");
        assertThat(dto.getLicensePlate()).isEqualTo("OLD-001");
    }

    @Test
    void toggleOnlineStatus_goOnline() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDTO dto = service.toggleOnlineStatus(userId, true);

        assertThat(dto.getIsOnline()).isTrue();
        assertThat(existingProfile.getLastSeenAt()).isNotNull();
    }

    @Test
    void toggleOnlineStatus_goOffline() {
        existingProfile.setIsOnline(true);
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DriverProfileDTO dto = service.toggleOnlineStatus(userId, false);

        assertThat(dto.getIsOnline()).isFalse();
    }

    @Test
    void updateLocation_setsGps() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateLocation(userId, 40.7128, -74.0060);

        assertThat(existingProfile.getCurrentLat().doubleValue()).isCloseTo(40.7128, org.assertj.core.data.Offset.offset(0.001));
        assertThat(existingProfile.getCurrentLng().doubleValue()).isCloseTo(-74.006, org.assertj.core.data.Offset.offset(0.001));
        assertThat(existingProfile.getLastSeenAt()).isNotNull();
    }

    @Test
    void recordDeliveryComplete_successfulDelivery_incrementsTotal() {
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordDeliveryComplete(userId, true);

        assertThat(existingProfile.getTotalDeliveries()).isEqualTo(1);
        // success rate stays at 100 for a successful delivery
        assertThat(existingProfile.getSuccessRate().doubleValue()).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void recordDeliveryComplete_failedDelivery_reducesSuccessRate() {
        existingProfile.setTotalDeliveries(9);
        existingProfile.setSuccessRate(new BigDecimal("100.00"));
        when(driverProfileRepository.findByUserId(userId)).thenReturn(Optional.of(existingProfile));
        when(driverProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.recordDeliveryComplete(userId, false);

        assertThat(existingProfile.getTotalDeliveries()).isEqualTo(10);
        // 100% * 9 / 10 = 90%
        assertThat(existingProfile.getSuccessRate().doubleValue()).isCloseTo(90.0, org.assertj.core.data.Offset.offset(0.1));
    }
}
