package com.quickbite.vendors.repository;

import com.quickbite.vendors.entity.ModifierGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for ModifierGroup entity operations.
 */
@Repository
public interface ModifierGroupRepository extends JpaRepository<ModifierGroup, UUID> {

    /**
     * Find modifier groups for a menu item, ordered by sortOrder.
     *
     * @param menuItemId the menu item ID
     * @return ordered list of modifier groups
     */
    List<ModifierGroup> findByMenuItemIdOrderBySortOrder(UUID menuItemId);

    /**
     * Delete all modifier groups for a menu item.
     *
     * @param menuItemId the menu item ID
     */
    void deleteByMenuItemId(UUID menuItemId);
}
