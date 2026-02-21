package com.quickbite.common.feature;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Binds {@code feature.flags.*} properties into a map.
 * Example: {@code feature.flags.driver-auto-assign=true} → key "driver-auto-assign" → true
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "feature")
public class FeatureFlagProperties {

    private Map<String, Boolean> flags = new HashMap<>();
}
