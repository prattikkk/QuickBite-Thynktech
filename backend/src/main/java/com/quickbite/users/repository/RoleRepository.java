package com.quickbite.users.repository;

import com.quickbite.users.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Role entity operations.
 * Provides CRUD and custom query methods for roles.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by its name.
     *
     * @param name the role name (e.g., "ADMIN", "CUSTOMER", "VENDOR", "DRIVER")
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(String name);

    /**
     * Check if a role exists by name.
     *
     * @param name the role name
     * @return true if role exists
     */
    boolean existsByName(String name);
}
