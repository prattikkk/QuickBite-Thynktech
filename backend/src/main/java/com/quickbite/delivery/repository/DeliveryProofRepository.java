package com.quickbite.delivery.repository;

import com.quickbite.delivery.entity.DeliveryProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for delivery proof records.
 */
@Repository
public interface DeliveryProofRepository extends JpaRepository<DeliveryProof, UUID> {

    Optional<DeliveryProof> findByOrderId(UUID orderId);

    List<DeliveryProof> findByDriverIdOrderBySubmittedAtDesc(UUID driverId);

    boolean existsByOrderId(UUID orderId);
}
