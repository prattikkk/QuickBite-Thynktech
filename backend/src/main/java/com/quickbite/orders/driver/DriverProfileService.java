package com.quickbite.orders.driver;

import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Service for driver profile operations — online/offline toggle, profile CRUD, location updates.
 * Phase 1 — Driver Dashboard Enhancement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;
    private final UserRepository userRepository;

    /**
     * Get or auto-create a driver profile for the given user.
     */
    @Transactional
    public DriverProfile getOrCreateProfile(UUID userId) {
        return driverProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
                    DriverProfile profile = DriverProfile.builder()
                            .user(user)
                            .vehicleType("MOTORCYCLE")
                            .isOnline(false)
                            .totalDeliveries(0)
                            .successRate(new BigDecimal("100.00"))
                            .build();
                    log.info("Auto-created driver profile for user {}", userId);
                    return driverProfileRepository.save(profile);
                });
    }

    /**
     * Get driver profile DTO for the given user.
     */
    @Transactional(readOnly = true)
    public DriverProfileDTO getProfileDTO(UUID userId) {
        DriverProfile profile = getOrCreateProfile(userId);
        return mapToDTO(profile);
    }

    /**
     * Update driver profile fields.
     */
    @Transactional
    public DriverProfileDTO updateProfile(UUID userId, String vehicleType, String licensePlate) {
        DriverProfile profile = getOrCreateProfile(userId);
        if (vehicleType != null) profile.setVehicleType(vehicleType);
        if (licensePlate != null) profile.setLicensePlate(licensePlate);
        driverProfileRepository.save(profile);
        log.info("Updated driver profile for user {}: vehicle={}, plate={}", userId, vehicleType, licensePlate);
        return mapToDTO(profile);
    }

    /**
     * Toggle online/offline status.
     */
    @Transactional
    public DriverProfileDTO toggleOnlineStatus(UUID userId, boolean online) {
        DriverProfile profile = getOrCreateProfile(userId);
        profile.setIsOnline(online);
        if (online) {
            profile.setLastSeenAt(OffsetDateTime.now());
        }
        driverProfileRepository.save(profile);
        log.info("Driver {} is now {}", userId, online ? "ONLINE" : "OFFLINE");
        return mapToDTO(profile);
    }

    /**
     * Update the driver's GPS location and mark them as recently seen.
     */
    @Transactional
    public void updateLocation(UUID userId, double lat, double lng) {
        DriverProfile profile = getOrCreateProfile(userId);
        profile.setCurrentLat(BigDecimal.valueOf(lat));
        profile.setCurrentLng(BigDecimal.valueOf(lng));
        profile.setLastSeenAt(OffsetDateTime.now());
        driverProfileRepository.save(profile);
    }

    /**
     * Start shift: set online, record shift_started_at, clear shift_ended_at.
     */
    @Transactional
    public DriverProfileDTO startShift(UUID userId) {
        DriverProfile profile = getOrCreateProfile(userId);
        profile.setIsOnline(true);
        profile.setShiftStartedAt(OffsetDateTime.now());
        profile.setShiftEndedAt(null);
        profile.setLastSeenAt(OffsetDateTime.now());
        driverProfileRepository.save(profile);
        log.info("Driver {} started shift", userId);
        return mapToDTO(profile);
    }

    /**
     * End shift: set offline, record shift_ended_at, clear current location.
     */
    @Transactional
    public DriverProfileDTO endShift(UUID userId) {
        DriverProfile profile = getOrCreateProfile(userId);
        profile.setIsOnline(false);
        profile.setShiftEndedAt(OffsetDateTime.now());
        profile.setCurrentLat(null);
        profile.setCurrentLng(null);
        driverProfileRepository.save(profile);
        log.info("Driver {} ended shift", userId);
        return mapToDTO(profile);
    }

    /**
     * Increment total deliveries and recalculate success rate.
     */
    @Transactional
    public void recordDeliveryComplete(UUID userId, boolean success) {
        DriverProfile profile = getOrCreateProfile(userId);
        int total = profile.getTotalDeliveries() + 1;
        profile.setTotalDeliveries(total);
        if (!success) {
            // Reduce success rate proportionally
            double current = profile.getSuccessRate().doubleValue();
            double newRate = ((current * (total - 1)) / total);
            profile.setSuccessRate(BigDecimal.valueOf(Math.max(0, newRate)));
        }
        driverProfileRepository.save(profile);
    }

    private DriverProfileDTO mapToDTO(DriverProfile profile) {
        return DriverProfileDTO.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .name(profile.getUser().getName())
                .email(profile.getUser().getEmail())
                .vehicleType(profile.getVehicleType())
                .licensePlate(profile.getLicensePlate())
                .isOnline(profile.getIsOnline())
                .currentLat(profile.getCurrentLat() != null ? profile.getCurrentLat().doubleValue() : null)
                .currentLng(profile.getCurrentLng() != null ? profile.getCurrentLng().doubleValue() : null)
                .totalDeliveries(profile.getTotalDeliveries())
                .successRate(profile.getSuccessRate())
                .shiftStartedAt(profile.getShiftStartedAt())
                .shiftEndedAt(profile.getShiftEndedAt())
                .build();
    }

    /**
     * List all currently online drivers (for vendor runner assignment).
     */
    @Transactional(readOnly = true)
    public java.util.List<DriverProfile> getOnlineDrivers() {
        return driverProfileRepository.findByIsOnlineTrue();
    }
}
