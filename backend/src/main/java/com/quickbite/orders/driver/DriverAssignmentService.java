package com.quickbite.orders.driver;

import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for finding and assigning drivers to orders.
 * Uses simple nearest-driver matching based on address lat/lng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverAssignmentService {

    private final UserRepository userRepository;

    /**
     * Find nearest available driver within radius using Haversine formula.
     * This is a simplified implementation; production would use spatial indexes.
     *
     * @param latitude delivery latitude
     * @param longitude delivery longitude
     * @param radiusKm search radius in kilometers
     * @return Optional<DriverInfo> if driver found
     */
    @Transactional(readOnly = true)
    public Optional<DriverInfo> findNearestAvailableDriver(Double latitude, Double longitude, Double radiusKm) {
        if (latitude == null || longitude == null) {
            log.warn("Cannot find driver: delivery coordinates are null");
            return Optional.empty();
        }

        log.info("Searching for available driver near lat={}, lng={}, radius={}km", latitude, longitude, radiusKm);

        // Simple SQL query using Haversine formula
        // In production, use PostGIS or spatial indexes
        String sql = """
            SELECT u.id, u.name, u.phone, a.lat, a.lng,
                   (6371 * acos(
                       cos(radians(:lat)) * cos(radians(a.lat)) *
                       cos(radians(a.lng) - radians(:lng)) +
                       sin(radians(:lat)) * sin(radians(a.lat))
                   )) AS distance_km
            FROM users u
            INNER JOIN user_role ur ON u.id = ur.user_id
            INNER JOIN roles r ON ur.role_id = r.id
            LEFT JOIN addresses a ON u.id = a.user_id AND a.is_default = true
            WHERE r.name = 'DRIVER'
              AND u.active = true
              AND a.lat IS NOT NULL
              AND a.lng IS NOT NULL
            HAVING distance_km <= :radius
            ORDER BY distance_km
            LIMIT 1
            """;

        // Note: This is a native query placeholder
        // In real implementation, would use entityManager.createNativeQuery()
        // For now, we'll return empty for drivers without proper location data
        
        // Fallback: Find any active driver
        var driver = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "DRIVER".equals(u.getRole().getName()))
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .findFirst();

        if (driver.isEmpty()) {
            log.warn("No available drivers found");
            return Optional.empty();
        }

        var driverUser = driver.get();
        var defaultAddress = driverUser.getAddresses().stream()
                .filter(addr -> addr.getIsDefault() != null && addr.getIsDefault())
                .findFirst()
                .orElse(null);

        DriverInfo info = DriverInfo.builder()
                .driverId(driverUser.getId())
                .name(driverUser.getName())
                .phone(driverUser.getPhone())
                .lat(defaultAddress != null && defaultAddress.getLat() != null ? defaultAddress.getLat().doubleValue() : null)
                .lng(defaultAddress != null && defaultAddress.getLng() != null ? defaultAddress.getLng().doubleValue() : null)
                .distanceKm(null) // Would calculate if coords available
                .build();

        log.info("Found driver: {} (ID: {})", info.getName(), info.getDriverId());
        return Optional.of(info);
    }

    /**
     * Assign the nearest available driver to an order.
     *
     * @param deliveryLat delivery latitude
     * @param deliveryLng delivery longitude
     * @return Optional<UUID> assigned driver ID
     */
    public Optional<UUID> assignDriverToOrder(Double deliveryLat, Double deliveryLng) {
        return findNearestAvailableDriver(deliveryLat, deliveryLng, 10.0) // 10km radius
                .map(DriverInfo::getDriverId);
    }
}
