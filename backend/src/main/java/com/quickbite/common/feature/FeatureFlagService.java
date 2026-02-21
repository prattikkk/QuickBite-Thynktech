package com.quickbite.common.feature;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Feature flag service with layered resolution:
 * <ol>
 *   <li>Database value (runtime-mutable via admin API)</li>
 *   <li>Application properties default ({@code feature.flags.*})</li>
 *   <li>Hard-coded default ({@code false})</li>
 * </ol>
 * The DB layer is cached in memory and refreshed on write.
 * Properties defaults are loaded from {@code feature.flags.*} prefix.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private final FeatureFlagRepository flagRepository;
    private final FeatureFlagProperties flagProperties;

    /** In-memory cache of DB flags — refreshed on toggle. */
    private final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void loadFromDb() {
        flagRepository.findAll().forEach(f -> cache.put(f.getFlagKey(), f.isEnabled()));
        log.info("Loaded {} feature flags from database", cache.size());
    }

    /**
     * Check if a feature is enabled.
     * Resolution: DB cache → properties default → false.
     */
    public boolean isEnabled(String key) {
        // 1. DB cache
        Boolean dbValue = cache.get(key);
        if (dbValue != null) return dbValue;

        // 2. Properties default
        Boolean propValue = flagProperties.getFlags().get(key);
        if (propValue != null) return propValue;

        // 3. Hard-coded default
        return false;
    }

    /**
     * Toggle a flag at runtime. Persists to DB and updates cache.
     */
    public FeatureFlag toggle(String key, boolean enabled, String updatedBy) {
        FeatureFlag flag = flagRepository.findById(key)
                .orElse(FeatureFlag.builder()
                        .flagKey(key)
                        .description("Created via admin API")
                        .build());
        flag.setEnabled(enabled);
        flag.setUpdatedBy(updatedBy);
        flag = flagRepository.save(flag);
        cache.put(key, enabled);
        log.info("Feature flag '{}' set to {} by {}", key, enabled, updatedBy);
        return flag;
    }

    /**
     * Return all known flags with their effective values.
     */
    public Map<String, Boolean> getAllFlags() {
        Map<String, Boolean> result = new HashMap<>(flagProperties.getFlags());
        // DB overrides properties
        cache.forEach(result::put);
        return result;
    }

    /**
     * Return all persisted flags with metadata.
     */
    public List<FeatureFlag> getAllFlagEntities() {
        return flagRepository.findAll();
    }
}
