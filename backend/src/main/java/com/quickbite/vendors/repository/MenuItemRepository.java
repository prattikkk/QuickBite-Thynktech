package com.quickbite.vendors.repository;

import com.quickbite.vendors.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for MenuItem entity operations.
 * Provides menu item search and filtering by vendor.
 */
@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    /**
     * Find all menu items for a vendor.
     *
     * @param vendorId the vendor ID
     * @return List of menu items
     */
    List<MenuItem> findByVendorId(UUID vendorId);

    /**
     * Find available menu items for a vendor.
     *
     * @param vendorId the vendor ID
     * @return List of available menu items
     */
    List<MenuItem> findByVendorIdAndAvailableTrue(UUID vendorId);

    /**
     * Find menu items by category for a vendor.
     *
     * @param vendorId the vendor ID
     * @param category the category name
     * @return List of menu items in the specified category
     */
    List<MenuItem> findByVendorIdAndCategory(UUID vendorId, String category);

    /**
     * Find available menu items by category for a vendor.
     *
     * @param vendorId the vendor ID
     * @param category the category name
     * @return List of available menu items in the specified category
     */
    List<MenuItem> findByVendorIdAndCategoryAndAvailableTrue(UUID vendorId, String category);

    /**
     * Search menu items by name (case-insensitive partial match).
     *
     * @param searchTerm the search term
     * @param vendorId the vendor ID
     * @return List of matching menu items
     */
    @Query("SELECT m FROM MenuItem m WHERE m.vendor.id = :vendorId " +
           "AND LOWER(m.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "AND m.available = true")
    List<MenuItem> searchMenuItems(@Param("searchTerm") String searchTerm, @Param("vendorId") UUID vendorId);

    /**
     * Find menu items within a price range for a vendor.
     *
     * @param vendorId the vendor ID
     * @param minPrice minimum price in cents
     * @param maxPrice maximum price in cents
     * @return List of menu items in the specified price range
     */
    @Query("SELECT m FROM MenuItem m WHERE m.vendor.id = :vendorId " +
           "AND m.priceCents BETWEEN :minPrice AND :maxPrice " +
           "AND m.available = true " +
           "ORDER BY m.priceCents ASC")
    List<MenuItem> findByPriceRange(
        @Param("vendorId") UUID vendorId,
        @Param("minPrice") Long minPrice,
        @Param("maxPrice") Long maxPrice
    );

    /**
     * Get all distinct categories for a vendor.
     *
     * @param vendorId the vendor ID
     * @return List of category names
     */
    @Query("SELECT DISTINCT m.category FROM MenuItem m WHERE m.vendor.id = :vendorId AND m.category IS NOT NULL")
    List<String> findCategoriesByVendorId(@Param("vendorId") UUID vendorId);

    /**
     * Count available menu items for a vendor.
     *
     * @param vendorId the vendor ID
     * @return count of available menu items
     */
    Long countByVendorIdAndAvailableTrue(UUID vendorId);

    /**
     * Find menu items with pagination.
     *
     * @param vendorId the vendor ID
     * @param pageable pagination information
     * @return Page of menu items
     */
    Page<MenuItem> findByVendorId(UUID vendorId, Pageable pageable);
}
