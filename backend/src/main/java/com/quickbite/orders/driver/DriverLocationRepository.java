package com.quickbite.orders.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for driver GPS location samples.
 */
@Repository
public interface DriverLocationRepository extends JpaRepository<DriverLocation, UUID> {

    /**
     * Return the N most recent location points for a driver, newest first.
     */
    List<DriverLocation> findTop20ByDriverIdOrderByRecordedAtDesc(UUID driverId);

    /**
     * Return this driver's most recent location point.
     */
    DriverLocation findFirstByDriverIdOrderByRecordedAtDesc(UUID driverId);

    /**
     * Count how many samples a driver has recorded in the given window.
     * Used for server-side rate-limit validation.
     */
    @Query("SELECT COUNT(dl) FROM DriverLocation dl WHERE dl.driver.id = :driverId AND dl.recordedAt >= :since")
    long countByDriverIdSince(UUID driverId, OffsetDateTime since);

    /**
     * Prune old location samples older than the given cutoff.
     */
    @Modifying
    @Query("DELETE FROM DriverLocation dl WHERE dl.recordedAt < :cutoff")
    int deleteOlderThan(OffsetDateTime cutoff);
}
