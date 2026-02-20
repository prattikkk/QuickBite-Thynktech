package com.quickbite.users.repository;

import com.quickbite.users.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * Provides CRUD and custom query methods for users.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find a user by email address.
     *
     * @param email the user's email
     * @return Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by email and active status.
     *
     * @param email the user's email
     * @param active the active status
     * @return Optional containing the user if found
     */
    Optional<User> findByEmailAndActive(String email, Boolean active);

    /**
     * Check if a user exists by email.
     *
     * @param email the email to check
     * @return true if user exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by role ID.
     *
     * @param roleId the role ID
     * @param pageable pagination information
     * @return Page of users with the specified role
     */
    Page<User> findByRoleId(UUID roleId, Pageable pageable);

    /**
     * Find active users by role name.
     *
     * @param roleName the role name
     * @return List of active users with the specified role
     */
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.active = true")
    List<User> findActiveUsersByRoleName(@Param("roleName") String roleName);

    /**
     * Find users by name containing (case-insensitive search).
     *
     * @param name the name search term
     * @param pageable pagination information
     * @return Page of matching users
     */
    Page<User> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Count active users by role.
     *
     * @param roleId the role ID
     * @return count of active users
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.role.id = :roleId AND u.active = true")
    Long countActiveUsersByRole(@Param("roleId") UUID roleId);
}
