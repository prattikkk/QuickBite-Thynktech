package com.quickbite.common.feature;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Persistent feature flag stored in the database.
 * Allows runtime toggling without redeploy.
 */
@Entity
@Table(name = "feature_flags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureFlag {

    @Id
    @Column(name = "flag_key", length = 100)
    private String flagKey;

    @Column(nullable = false)
    private boolean enabled;

    @Column(length = 500)
    private String description;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @PrePersist
    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
