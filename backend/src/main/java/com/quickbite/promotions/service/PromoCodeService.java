package com.quickbite.promotions.service;

import com.quickbite.common.feature.FeatureFlagService;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.promotions.dto.PromoCodeDTO;
import com.quickbite.promotions.dto.PromoCreateRequest;
import com.quickbite.promotions.dto.PromoValidateResponse;
import com.quickbite.promotions.entity.DiscountType;
import com.quickbite.promotions.entity.PromoCode;
import com.quickbite.promotions.repository.PromoCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Promo code engine — validate, calculate discount, admin CRUD.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final FeatureFlagService featureFlagService;

    // ========== Customer-facing ==========

    /**
     * Validate a promo code against a subtotal and return computed discount.
     */
    @Transactional(readOnly = true)
    public PromoValidateResponse validatePromo(String code, long subtotalCents) {
        if (!featureFlagService.isEnabled("promo-engine")) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("Promo codes are currently disabled").build();
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (promo == null) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("Invalid promo code").build();
        }

        // Active check
        if (!Boolean.TRUE.equals(promo.getActive())) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("This promo code is no longer active").build();
        }

        // Date validity
        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(promo.getValidFrom())) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("This promo code is not yet valid").build();
        }
        if (promo.getValidUntil() != null && now.isAfter(promo.getValidUntil())) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("This promo code has expired").build();
        }

        // Usage limit
        if (promo.getMaxUses() != null && promo.getCurrentUses() >= promo.getMaxUses()) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code).message("This promo code has reached its usage limit").build();
        }

        // Minimum order
        if (subtotalCents < promo.getMinOrderCents()) {
            return PromoValidateResponse.builder()
                    .valid(false).code(code)
                    .message("Minimum order of ₹" + (promo.getMinOrderCents() / 100) + " required")
                    .build();
        }

        long discountCents = calculateDiscount(promo, subtotalCents);

        return PromoValidateResponse.builder()
                .valid(true)
                .code(promo.getCode())
                .description(promo.getDescription())
                .discountType(promo.getDiscountType().name())
                .discountCents(discountCents)
                .message("Promo applied! You save ₹" + (discountCents / 100))
                .build();
    }

    /**
     * Apply a promo code — validate + increment usage. Returns discount in cents.
     * Called from OrderService during order creation.
     */
    @Transactional
    public long applyPromo(String code, long subtotalCents) {
        if (!featureFlagService.isEnabled("promo-engine")) {
            return 0;
        }

        PromoCode promo = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BusinessException("Invalid promo code: " + code));

        PromoValidateResponse result = validatePromo(code, subtotalCents);
        if (!result.isValid()) {
            throw new BusinessException(result.getMessage());
        }

        promo.incrementUses();
        promoCodeRepository.save(promo);

        log.info("Promo '{}' applied — discount {} cents on subtotal {} cents",
                promo.getCode(), result.getDiscountCents(), subtotalCents);

        return result.getDiscountCents();
    }

    /**
     * Calculate discount amount in cents.
     */
    public long calculateDiscount(PromoCode promo, long subtotalCents) {
        long discount;
        if (promo.getDiscountType() == DiscountType.FIXED) {
            discount = promo.getDiscountValue();
        } else {
            // PERCENT: discountValue is in basis points (1500 = 15%)
            discount = subtotalCents * promo.getDiscountValue() / 10_000;
        }
        // Apply cap
        if (promo.getMaxDiscountCents() != null && discount > promo.getMaxDiscountCents()) {
            discount = promo.getMaxDiscountCents();
        }
        // Discount cannot exceed subtotal
        return Math.min(discount, subtotalCents);
    }

    // ========== Admin CRUD ==========

    @Transactional
    public PromoCodeDTO createPromo(PromoCreateRequest req) {
        if (promoCodeRepository.existsByCodeIgnoreCase(req.getCode().trim())) {
            throw new BusinessException("Promo code already exists: " + req.getCode());
        }

        PromoCode promo = PromoCode.builder()
                .code(req.getCode().trim().toUpperCase())
                .description(req.getDescription())
                .discountType(DiscountType.valueOf(req.getDiscountType().toUpperCase()))
                .discountValue(req.getDiscountValue())
                .minOrderCents(req.getMinOrderCents() != null ? req.getMinOrderCents() : 0L)
                .maxDiscountCents(req.getMaxDiscountCents())
                .maxUses(req.getMaxUses())
                .validFrom(req.getValidFrom() != null ? req.getValidFrom() : OffsetDateTime.now())
                .validUntil(req.getValidUntil())
                .active(req.getActive() != null ? req.getActive() : true)
                .build();

        promo = promoCodeRepository.save(promo);
        log.info("Promo code created: {}", promo.getCode());
        return toDTO(promo);
    }

    @Transactional
    public PromoCodeDTO updatePromo(UUID id, PromoCreateRequest req) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo code not found: " + id));

        if (req.getDescription() != null) promo.setDescription(req.getDescription());
        if (req.getDiscountType() != null) promo.setDiscountType(DiscountType.valueOf(req.getDiscountType().toUpperCase()));
        if (req.getDiscountValue() != null) promo.setDiscountValue(req.getDiscountValue());
        if (req.getMinOrderCents() != null) promo.setMinOrderCents(req.getMinOrderCents());
        if (req.getMaxDiscountCents() != null) promo.setMaxDiscountCents(req.getMaxDiscountCents());
        if (req.getMaxUses() != null) promo.setMaxUses(req.getMaxUses());
        if (req.getValidFrom() != null) promo.setValidFrom(req.getValidFrom());
        if (req.getValidUntil() != null) promo.setValidUntil(req.getValidUntil());
        if (req.getActive() != null) promo.setActive(req.getActive());

        promo = promoCodeRepository.save(promo);
        log.info("Promo code updated: {}", promo.getCode());
        return toDTO(promo);
    }

    @Transactional(readOnly = true)
    public List<PromoCodeDTO> listAllPromos() {
        return promoCodeRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromoCodeDTO> listActivePromos() {
        return promoCodeRepository.findByActiveTrueOrderByCreatedAtDesc().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PromoCodeDTO getPromo(UUID id) {
        PromoCode promo = promoCodeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Promo code not found: " + id));
        return toDTO(promo);
    }

    @Transactional
    public void deletePromo(UUID id) {
        promoCodeRepository.deleteById(id);
        log.info("Promo code deleted: {}", id);
    }

    private PromoCodeDTO toDTO(PromoCode p) {
        return PromoCodeDTO.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .minOrderCents(p.getMinOrderCents())
                .maxDiscountCents(p.getMaxDiscountCents())
                .maxUses(p.getMaxUses())
                .currentUses(p.getCurrentUses())
                .validFrom(p.getValidFrom())
                .validUntil(p.getValidUntil())
                .active(p.getActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
