package com.quickbite.vendors.repository;

import com.quickbite.vendors.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Vendor entity operations.
 * Provides vendor search, filtering, and location-based queries.
 */
@Repository
public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    /**
     * Find active vendors by name (case-insensitive partial match).
     *
     * @param name the search term
     * @param pageable pagination information
     * @return Page of matching vendors
     */
    Page<Vendor> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    /**
     * Find a vendor by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the vendor if found
     */
    Optional<Vendor> findByUserId(UUID userId);

    /**
     * Find all active vendors.
     *
     * @param pageable pagination information
     * @return Page of active vendors
     */
    Page<Vendor> findByActiveTrue(Pageable pageable);

    /**
     * Find vendors by minimum rating.
     *
     * @param minRating minimum rating threshold
     * @param pageable pagination information
     * @return Page of vendors with rating >= minRating
     */
    @Query("SELECT v FROM Vendor v WHERE v.rating >= :minRating AND v.active = true ORDER BY v.rating DESC")
    Page<Vendor> findByMinimumRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    /**
     * Find vendors near a location (simple bounding box query).
     * For production, consider using PostGIS for proper geospatial queries.
     *
     * @param minLat minimum latitude
     * @param maxLat maximum latitude
     * @param minLng minimum longitude
     * @param maxLng maximum longitude
     * @param pageable pagination information
     * @return Page of vendors in the specified area
     */
    @Query("SELECT v FROM Vendor v WHERE v.active = true " +
           "AND v.lat BETWEEN :minLat AND :maxLat " +
           "AND v.lng BETWEEN :minLng AND :maxLng")
    Page<Vendor> findVendorsNearLocation(
        @Param("minLat") BigDecimal minLat,
        @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,
        @Param("maxLng") BigDecimal maxLng,
        Pageable pageable
    );

    /**
     * Count active vendors.
     *
     * @return count of active vendors
     */
    Long countByActiveTrue();

    /**
     * Search vendors by name with minimum rating filter.
     *
     * @param searchTerm search term for name
     * @param minRating minimum rating
     * @param pageable pagination information
     * @return Page of matching vendors
     */
    @Query("SELECT v FROM Vendor v WHERE v.active = true " +
           "AND LOWER(v.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND v.rating >= :minRating " +
           "ORDER BY v.rating DESC")
    Page<Vendor> searchVendors(
        @Param("searchTerm") String searchTerm,
        @Param("minRating") BigDecimal minRating,
        Pageable pageable
    );
}
