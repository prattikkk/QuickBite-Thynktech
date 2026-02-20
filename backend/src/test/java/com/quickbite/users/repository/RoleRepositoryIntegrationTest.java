package com.quickbite.users.repository;

import com.quickbite.BaseIntegrationTest;
import com.quickbite.users.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for RoleRepository using Testcontainers.
 * Verifies that roles are loaded from Flyway migration and can be queried.
 */
@SpringBootTest
@Transactional
class RoleRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Should find role by name - ADMIN")
    void shouldFindAdminRole() {
        // Given: Roles inserted by Flyway migration
        String roleName = "ADMIN";

        // When
        Optional<Role> role = roleRepository.findByName(roleName);

        // Then
        assertThat(role).isPresent();
        assertThat(role.get().getName()).isEqualTo(roleName);
        assertThat(role.get().getId()).isNotNull();
    }

    @Test
    @DisplayName("Should find all four roles")
    void shouldFindAllRoles() {
        // When
        var roles = roleRepository.findAll();

        // Then
        assertThat(roles).hasSize(4);
        assertThat(roles).extracting(Role::getName)
                .containsExactlyInAnyOrder("ADMIN", "VENDOR", "CUSTOMER", "DRIVER");
    }

    @Test
    @DisplayName("Should check if role exists by name")
    void shouldCheckRoleExistsByName() {
        // When & Then
        assertThat(roleRepository.existsByName("ADMIN")).isTrue();
        assertThat(roleRepository.existsByName("CUSTOMER")).isTrue();
        assertThat(roleRepository.existsByName("VENDOR")).isTrue();
        assertThat(roleRepository.existsByName("DRIVER")).isTrue();
        assertThat(roleRepository.existsByName("NONEXISTENT")).isFalse();
    }

    @Test
    @DisplayName("Should find each role using constants")
    void shouldFindRolesByConstants() {
        // When & Then
        assertThat(roleRepository.findByName(Role.ADMIN)).isPresent();
        assertThat(roleRepository.findByName(Role.VENDOR)).isPresent();
        assertThat(roleRepository.findByName(Role.CUSTOMER)).isPresent();
        assertThat(roleRepository.findByName(Role.DRIVER)).isPresent();
    }
}
