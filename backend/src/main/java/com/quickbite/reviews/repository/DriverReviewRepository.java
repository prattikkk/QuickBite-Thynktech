package com.quickbite.reviews.repository;

import com.quickbite.reviews.entity.DriverReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverReviewRepository extends JpaRepository<DriverReview, UUID> {

    Page<DriverReview> findByDriverIdAndHiddenFalseOrderByCreatedAtDesc(UUID driverId, Pageable pageable);

    Optional<DriverReview> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    boolean existsByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    long countByDriverIdAndHiddenFalse(UUID driverId);

    @Query("SELECT AVG(r.rating) FROM DriverReview r WHERE r.driver.id = :driverId AND r.hidden = false")
    Double findAverageRatingByDriverId(UUID driverId);

    Page<DriverReview> findByDriverIdAndDisputedTrue(UUID driverId, Pageable pageable);
}
