package com.quickbite.vendors.service;

import com.quickbite.orders.exception.BusinessException;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.entity.VendorCommission;
import com.quickbite.vendors.repository.VendorCommissionRepository;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing vendor commission rates and calculating commissions.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorCommissionService {

    private final VendorCommissionRepository commissionRepository;
    private final VendorRepository vendorRepository;

    /** Default commission rate in basis points (15%). */
    @Value("${commission.default-rate-bps:1500}")
    private int defaultRateBps;

    /** Default flat fee per order in cents. */
    @Value("${commission.default-flat-fee-cents:0}")
    private long defaultFlatFeeCents;

    /**
     * Calculate commission for an order.
     * Returns { commissionCents, vendorPayoutCents }.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> calculateCommission(UUID vendorId, long subtotalCents) {
        VendorCommission commission = commissionRepository.findActiveByVendorId(vendorId)
                .orElse(null);

        long commissionCents;
        if (commission != null) {
            commissionCents = commission.calculateCommission(subtotalCents);
        } else {
            commissionCents = subtotalCents * defaultRateBps / 10_000 + defaultFlatFeeCents;
        }

        long vendorPayout = subtotalCents - commissionCents;
        if (vendorPayout < 0) vendorPayout = 0;

        return Map.of(
                "commissionCents", commissionCents,
                "vendorPayoutCents", vendorPayout
        );
    }

    /**
     * Set commission rate for a vendor (admin operation).
     */
    @Transactional
    public VendorCommission setCommission(UUID vendorId, int rateBps, long flatFeeCents) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found: " + vendorId));

        // Deactivate current commission
        commissionRepository.findActiveByVendorId(vendorId)
                .ifPresent(existing -> {
                    existing.setEffectiveUntil(OffsetDateTime.now());
                    commissionRepository.save(existing);
                });

        VendorCommission commission = VendorCommission.builder()
                .vendor(vendor)
                .commissionRateBps(rateBps)
                .flatFeeCents(flatFeeCents)
                .effectiveFrom(OffsetDateTime.now())
                .build();

        commission = commissionRepository.save(commission);
        log.info("Commission set for vendor {}: {}bps + {}c flat", vendorId, rateBps, flatFeeCents);
        return commission;
    }

    /**
     * Get current commission rate for a vendor.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getCommissionRate(UUID vendorId) {
        VendorCommission commission = commissionRepository.findActiveByVendorId(vendorId)
                .orElse(null);

        if (commission != null) {
            return Map.of(
                    "vendorId", vendorId,
                    "commissionRateBps", commission.getCommissionRateBps(),
                    "flatFeeCents", commission.getFlatFeeCents(),
                    "effectiveFrom", commission.getEffectiveFrom()
            );
        }

        return Map.of(
                "vendorId", vendorId,
                "commissionRateBps", defaultRateBps,
                "flatFeeCents", defaultFlatFeeCents,
                "source", "default"
        );
    }
}
