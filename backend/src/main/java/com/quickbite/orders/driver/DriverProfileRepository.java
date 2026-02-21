package com.quickbite.orders.driver;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for DriverProfile entity.
 */
@Repository
public interface DriverProfileRepository extends JpaRepository<DriverProfile, UUID> {

    Optional<DriverProfile> findByUserId(UUID userId);

    List<DriverProfile> findByIsOnlineTrue();

    boolean existsByUserId(UUID userId);
}
