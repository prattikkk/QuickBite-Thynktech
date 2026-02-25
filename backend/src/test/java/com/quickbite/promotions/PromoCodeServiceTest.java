package com.quickbite.promotions;

import com.quickbite.common.feature.FeatureFlagService;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.promotions.dto.PromoCodeDTO;
import com.quickbite.promotions.dto.PromoCreateRequest;
import com.quickbite.promotions.dto.PromoValidateResponse;
import com.quickbite.promotions.entity.DiscountType;
import com.quickbite.promotions.entity.PromoCode;
import com.quickbite.promotions.repository.PromoCodeRepository;
import com.quickbite.promotions.repository.PromoUsageRepository;
import com.quickbite.promotions.service.PromoCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PromoCodeService — Phase 3 promo engine.
 */
@ExtendWith(MockitoExtension.class)
class PromoCodeServiceTest {

    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private PromoUsageRepository promoUsageRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private FeatureFlagService featureFlagService;

    private PromoCodeService promoCodeService;

    private PromoCode percentPromo;
    private PromoCode fixedPromo;

    @BeforeEach
    void setUp() {
        promoCodeService = new PromoCodeService(promoCodeRepository, promoUsageRepository, orderRepository, featureFlagService);

        percentPromo = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("SAVE15")
                .description("15% off")
                .discountType(DiscountType.PERCENT)
                .discountValue(1500L) // 15% in basis points
                .minOrderCents(10000L) // ₹100 minimum
                .maxDiscountCents(25000L) // ₹250 cap
                .maxUses(100)
                .currentUses(0)
                .validFrom(OffsetDateTime.now().minusDays(1))
                .validUntil(OffsetDateTime.now().plusDays(30))
                .active(true)
                .build();

        fixedPromo = PromoCode.builder()
                .id(UUID.randomUUID())
                .code("FLAT50")
                .description("₹50 off")
                .discountType(DiscountType.FIXED)
                .discountValue(5000L) // ₹50 in cents
                .minOrderCents(20000L) // ₹200 minimum
                .maxDiscountCents(null) // no cap for fixed
                .maxUses(null) // unlimited
                .currentUses(10)
                .validFrom(OffsetDateTime.now().minusDays(7))
                .validUntil(null) // no expiry
                .active(true)
                .build();
    }

    // ========== validatePromo tests ==========

    @Test
    @DisplayName("validatePromo — valid percent code returns correct discount")
    void validatePromo_percentCode_success() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));

        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 100000L);

        assertThat(res.isValid()).isTrue();
        assertThat(res.getDiscountCents()).isEqualTo(15000L); // 15% of 100000
        assertThat(res.getCode()).isEqualTo("SAVE15");
    }

    @Test
    @DisplayName("validatePromo — percent code with cap limits discount")
    void validatePromo_percentCode_cappedAtMax() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));

        // 15% of 200000 = 30000, capped at 25000
        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 200000L);

        assertThat(res.isValid()).isTrue();
        assertThat(res.getDiscountCents()).isEqualTo(25000L);
    }

    @Test
    @DisplayName("validatePromo — valid fixed code returns exact discount")
    void validatePromo_fixedCode_success() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("FLAT50")).thenReturn(Optional.of(fixedPromo));

        PromoValidateResponse res = promoCodeService.validatePromo("FLAT50", 50000L);

        assertThat(res.isValid()).isTrue();
        assertThat(res.getDiscountCents()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("validatePromo — feature disabled returns invalid")
    void validatePromo_featureDisabled() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(false);

        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 100000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("disabled");
        verify(promoCodeRepository, never()).findByCodeIgnoreCase(any());
    }

    @Test
    @DisplayName("validatePromo — invalid code returns invalid")
    void validatePromo_invalidCode() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("BOGUS")).thenReturn(Optional.empty());

        PromoValidateResponse res = promoCodeService.validatePromo("BOGUS", 100000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("Invalid");
    }

    @Test
    @DisplayName("validatePromo — inactive code returns invalid")
    void validatePromo_inactiveCode() {
        percentPromo.setActive(false);
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));

        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 100000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("no longer active");
    }

    @Test
    @DisplayName("validatePromo — expired code returns invalid")
    void validatePromo_expiredCode() {
        percentPromo.setValidUntil(OffsetDateTime.now().minusDays(1));
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));

        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 100000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("expired");
    }

    @Test
    @DisplayName("validatePromo — usage limit reached returns invalid")
    void validatePromo_usageLimitReached() {
        percentPromo.setCurrentUses(100); // maxUses = 100
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));

        PromoValidateResponse res = promoCodeService.validatePromo("SAVE15", 100000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("usage limit");
    }

    @Test
    @DisplayName("validatePromo — subtotal below minimum returns invalid")
    void validatePromo_belowMinimum() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("FLAT50")).thenReturn(Optional.of(fixedPromo));

        // minOrderCents = 20000, subtotal = 15000
        PromoValidateResponse res = promoCodeService.validatePromo("FLAT50", 15000L);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getMessage()).contains("Minimum order");
    }

    // ========== applyPromo tests ==========

    @Test
    @DisplayName("applyPromo — valid code increments usage and returns discount")
    void applyPromo_success() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("SAVE15")).thenReturn(Optional.of(percentPromo));
        when(promoCodeRepository.save(any())).thenReturn(percentPromo);

        long discount = promoCodeService.applyPromo("SAVE15", 100000L);

        assertThat(discount).isEqualTo(15000L);
        assertThat(percentPromo.getCurrentUses()).isEqualTo(1);
        verify(promoCodeRepository).save(percentPromo);
    }

    @Test
    @DisplayName("applyPromo — feature disabled returns 0")
    void applyPromo_featureDisabled() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(false);

        long discount = promoCodeService.applyPromo("SAVE15", 100000L);

        assertThat(discount).isZero();
        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyPromo — invalid code throws BusinessException")
    void applyPromo_invalidCode_throws() {
        when(featureFlagService.isEnabled("promo-engine")).thenReturn(true);
        when(promoCodeRepository.findByCodeIgnoreCase("BOGUS")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.applyPromo("BOGUS", 100000L))
                .isInstanceOf(BusinessException.class);
    }

    // ========== calculateDiscount tests ==========

    @Test
    @DisplayName("calculateDiscount — fixed discount cannot exceed subtotal")
    void calculateDiscount_fixedCappedAtSubtotal() {
        PromoCode bigFixed = PromoCode.builder()
                .discountType(DiscountType.FIXED)
                .discountValue(50000L) // ₹500
                .maxDiscountCents(null)
                .build();

        long discount = promoCodeService.calculateDiscount(bigFixed, 30000L);
        assertThat(discount).isEqualTo(30000L); // capped at subtotal
    }

    // ========== Admin CRUD tests ==========

    @Test
    @DisplayName("createPromo — success creates and returns DTO")
    void createPromo_success() {
        PromoCreateRequest req = PromoCreateRequest.builder()
                .code("NEW20")
                .description("20% off")
                .discountType("PERCENT")
                .discountValue(2000L)
                .minOrderCents(5000L)
                .build();

        when(promoCodeRepository.existsByCodeIgnoreCase("NEW20")).thenReturn(false);
        when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> {
            PromoCode p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(OffsetDateTime.now());
            return p;
        });

        PromoCodeDTO dto = promoCodeService.createPromo(req);

        assertThat(dto.getCode()).isEqualTo("NEW20");
        assertThat(dto.getDiscountType()).isEqualTo(DiscountType.PERCENT);
        verify(promoCodeRepository).save(any(PromoCode.class));
    }

    @Test
    @DisplayName("createPromo — duplicate code throws exception")
    void createPromo_duplicateCode_throws() {
        PromoCreateRequest req = PromoCreateRequest.builder()
                .code("SAVE15")
                .discountType("PERCENT")
                .discountValue(1500L)
                .build();

        when(promoCodeRepository.existsByCodeIgnoreCase("SAVE15")).thenReturn(true);

        assertThatThrownBy(() -> promoCodeService.createPromo(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("listActivePromos — returns active promos")
    void listActivePromos_success() {
        when(promoCodeRepository.findByActiveTrueOrderByCreatedAtDesc())
                .thenReturn(List.of(percentPromo, fixedPromo));

        List<PromoCodeDTO> result = promoCodeService.listActivePromos();

        assertThat(result).hasSize(2);
    }
}
