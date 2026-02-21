package com.quickbite.common.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FeatureFlagService.
 */
@ExtendWith(MockitoExtension.class)
class FeatureFlagServiceTest {

    @Mock
    private FeatureFlagRepository flagRepository;

    private FeatureFlagProperties flagProperties;
    private FeatureFlagService service;

    @BeforeEach
    void setUp() {
        flagProperties = new FeatureFlagProperties();
        Map<String, Boolean> defaults = new HashMap<>();
        defaults.put("driver-auto-assign", true);
        defaults.put("promo-engine", false);
        flagProperties.setFlags(defaults);

        when(flagRepository.findAll()).thenReturn(Collections.emptyList());

        service = new FeatureFlagService(flagRepository, flagProperties);
        service.loadFromDb(); // simulate @PostConstruct
    }

    @Test
    void isEnabled_returnsPropertyDefault_whenNoDbOverride() {
        assertThat(service.isEnabled("driver-auto-assign")).isTrue();
        assertThat(service.isEnabled("promo-engine")).isFalse();
    }

    @Test
    void isEnabled_returnsFalse_forUnknownFlag() {
        assertThat(service.isEnabled("nonexistent-flag")).isFalse();
    }

    @Test
    void isEnabled_returnsDbValue_overridingProperty() {
        // Simulate DB has promo-engine=true
        FeatureFlag dbFlag = FeatureFlag.builder()
                .flagKey("promo-engine")
                .enabled(true)
                .build();
        when(flagRepository.findById("promo-engine")).thenReturn(Optional.of(dbFlag));
        when(flagRepository.save(any())).thenReturn(dbFlag);

        service.toggle("promo-engine", true, "admin");

        assertThat(service.isEnabled("promo-engine")).isTrue();
    }

    @Test
    void toggle_persistsFlagAndUpdatesCache() {
        FeatureFlag saved = FeatureFlag.builder()
                .flagKey("new-flag")
                .enabled(true)
                .updatedBy("admin")
                .build();
        when(flagRepository.findById("new-flag")).thenReturn(Optional.empty());
        when(flagRepository.save(any())).thenReturn(saved);

        FeatureFlag result = service.toggle("new-flag", true, "admin");

        assertThat(result.getFlagKey()).isEqualTo("new-flag");
        assertThat(result.isEnabled()).isTrue();
        verify(flagRepository).save(any(FeatureFlag.class));
        // Cache should now return true
        assertThat(service.isEnabled("new-flag")).isTrue();
    }

    @Test
    void getAllFlags_mergesDbAndPropertyDefaults() {
        Map<String, Boolean> all = service.getAllFlags();

        assertThat(all).containsEntry("driver-auto-assign", true);
        assertThat(all).containsEntry("promo-engine", false);
    }

    @Test
    void toggle_disablesFlag() {
        FeatureFlag existing = FeatureFlag.builder()
                .flagKey("driver-auto-assign")
                .enabled(true)
                .build();
        when(flagRepository.findById("driver-auto-assign")).thenReturn(Optional.of(existing));
        when(flagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.toggle("driver-auto-assign", false, "admin");

        assertThat(service.isEnabled("driver-auto-assign")).isFalse();
    }
}
