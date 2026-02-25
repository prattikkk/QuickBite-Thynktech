package com.quickbite.vendors.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ModifierGroup entity representing a group of modifiers for a menu item.
 * E.g. "Size", "Extras", "Spice Level".
 */
@Entity
@Table(name = "modifier_groups", indexes = {
    @Index(name = "idx_modgroup_menu_item", columnList = "menu_item_id"),
    @Index(name = "idx_modgroup_sort", columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModifierGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "min_selections", nullable = false)
    @Builder.Default
    private Integer minSelections = 0;

    @Column(name = "max_selections", nullable = false)
    @Builder.Default
    private Integer maxSelections = 1;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Modifier> modifiers = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "timestamptz", updatable = false)
    private OffsetDateTime createdAt;

    // Helper methods
    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
        modifier.setGroup(this);
    }

    public void removeModifier(Modifier modifier) {
        modifiers.remove(modifier);
        modifier.setGroup(null);
    }
}
