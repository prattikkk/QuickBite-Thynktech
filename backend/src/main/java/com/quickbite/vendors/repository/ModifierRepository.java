package com.quickbite.vendors.repository;

import com.quickbite.vendors.entity.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Modifier entity operations.
 */
@Repository
public interface ModifierRepository extends JpaRepository<Modifier, UUID> {

    /**
     * Find modifiers for a group, ordered by sortOrder.
     *
     * @param groupId the modifier group ID
     * @return ordered list of modifiers
     */
    List<Modifier> findByGroupIdOrderBySortOrder(UUID groupId);
}
