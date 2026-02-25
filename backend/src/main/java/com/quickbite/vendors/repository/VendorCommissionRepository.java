package com.quickbite.vendors.repository;

import com.quickbite.vendors.entity.VendorCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VendorCommissionRepository extends JpaRepository<VendorCommission, UUID> {

    /**
     * Find the currently active commission for a vendor.
     */
    @Query("SELECT vc FROM VendorCommission vc WHERE vc.vendor.id = :vendorId " +
           "AND vc.effectiveUntil IS NULL ORDER BY vc.effectiveFrom DESC")
    Optional<VendorCommission> findActiveByVendorId(@Param("vendorId") UUID vendorId);
}
