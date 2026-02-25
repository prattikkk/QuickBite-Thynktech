package com.quickbite.reviews.repository;

import com.quickbite.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByVendorIdAndHiddenFalseOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);

    Optional<Review> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    boolean existsByOrderIdAndCustomerId(UUID orderId, UUID customerId);

    long countByVendorIdAndHiddenFalse(UUID vendorId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.vendor.id = :vendorId AND r.hidden = false")
    Double findAverageRatingByVendorId(UUID vendorId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.vendor.id = :vendorId AND r.hidden = false GROUP BY r.rating ORDER BY r.rating DESC")
    java.util.List<Object[]> findRatingDistributionByVendorId(UUID vendorId);

    Page<Review> findByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);
}
