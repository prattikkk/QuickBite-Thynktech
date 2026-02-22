package com.quickbite.orders.driver;

import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for recording and querying driver GPS location samples.
 * Phase 2 — Foreground Live Location.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLocationService {

    /** Max GPS samples per driver per minute (server-side guard). */
    private static final int MAX_SAMPLES_PER_MINUTE = 12;

    /** Location history retained for 24 hours. */
    private static final int RETENTION_HOURS = 24;

    private final DriverLocationRepository locationRepository;
    private final DriverProfileService profileService;
    private final UserRepository userRepository;

    /**
     * Record a GPS sample for a driver.
     *
     * @return the persisted DriverLocation, or null if rate-limited
     */
    @Transactional
    public DriverLocation recordLocation(UUID driverId, double lat, double lng,
                                          Double accuracy, Double speed, Double heading) {
        // Server-side rate guard: max N samples per minute
        OffsetDateTime oneMinuteAgo = OffsetDateTime.now().minusMinutes(1);
        long recentCount = locationRepository.countByDriverIdSince(driverId, oneMinuteAgo);
        if (recentCount >= MAX_SAMPLES_PER_MINUTE) {
            log.debug("Driver {} rate-limited on location: {} samples in last minute", driverId, recentCount);
            return null;
        }

        User driverUser = userRepository.findById(driverId)
                .orElseThrow(() -> new RuntimeException("Driver user not found: " + driverId));

        DriverLocation loc = DriverLocation.builder()
                .driver(driverUser)
                .lat(BigDecimal.valueOf(lat))
                .lng(BigDecimal.valueOf(lng))
                .accuracy(accuracy)
                .speed(speed)
                .heading(heading)
                .recordedAt(OffsetDateTime.now())
                .build();

        loc = locationRepository.save(loc);

        // Also update the DriverProfile with latest position + lastSeenAt
        profileService.updateLocation(driverId, lat, lng);

        return loc;
    }

    /**
     * Return the 20 most recent location points for a driver (newest first).
     */
    @Transactional(readOnly = true)
    public List<DriverLocationDTO> getRecentLocations(UUID driverId) {
        return locationRepository.findTop20ByDriverIdOrderByRecordedAtDesc(driverId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Return the driver's single most-recent sample.
     */
    @Transactional(readOnly = true)
    public DriverLocationDTO getLastLocation(UUID driverId) {
        DriverLocation loc = locationRepository.findFirstByDriverIdOrderByRecordedAtDesc(driverId);
        return loc != null ? toDTO(loc) : null;
    }

    /**
     * Start a driver's shift: set online, record shift_started_at.
     */
    @Transactional
    public DriverProfileDTO startShift(UUID driverId) {
        return profileService.startShift(driverId);
    }

    /**
     * End a driver's shift: set offline, record shift_ended_at.
     */
    @Transactional
    public DriverProfileDTO endShift(UUID driverId) {
        return profileService.endShift(driverId);
    }

    /**
     * Scheduled task: prune location samples older than RETENTION_HOURS.
     * Runs every hour.
     */
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void pruneOldLocations() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(RETENTION_HOURS);
        int deleted = locationRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Pruned {} driver location samples older than {} hours", deleted, RETENTION_HOURS);
        }
    }

    private DriverLocationDTO toDTO(DriverLocation loc) {
        return DriverLocationDTO.builder()
                .id(loc.getId())
                .driverId(loc.getDriver().getId())
                .lat(loc.getLat().doubleValue())
                .lng(loc.getLng().doubleValue())
                .accuracy(loc.getAccuracy())
                .speed(loc.getSpeed())
                .heading(loc.getHeading())
                .recordedAt(loc.getRecordedAt())
                .build();
    }
}
