package com.quickbite.users.repository;

import com.quickbite.BaseIntegrationTest;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository using Testcontainers.
 * Tests verify repository methods work correctly with real PostgreSQL database.
 */
@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Should find user by email")
    void shouldFindUserByEmail() {
        // Given: Sample data loaded from Flyway migration
        String email = "alice@quickbite.test";

        // When
        Optional<User> user = userRepository.findByEmail(email);

        // Then
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo(email);
        assertThat(user.get().getName()).isEqualTo("Alice Customer");
    }

    @Test
    @DisplayName("Should find active user by email")
    void shouldFindActiveUserByEmail() {
        // Given
        String email = "alice@quickbite.test";

        // When
        Optional<User> user = userRepository.findByEmailAndActive(email, true);

        // Then
        assertThat(user).isPresent();
        assertThat(user.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Should check if user exists by email")
    void shouldCheckUserExistsByEmail() {
        // Given
        String existingEmail = "alice@quickbite.test";
        String nonExistingEmail = "nonexistent@quickbite.test";

        // When & Then
        assertThat(userRepository.existsByEmail(existingEmail)).isTrue();
        assertThat(userRepository.existsByEmail(nonExistingEmail)).isFalse();
    }

    @Test
    @DisplayName("Should find active users by role name")
    void shouldFindActiveUsersByRoleName() {
        // Given
        String roleName = "CUSTOMER";

        // When
        var users = userRepository.findActiveUsersByRoleName(roleName);

        // Then
        assertThat(users).isNotEmpty();
        assertThat(users).allMatch(u -> u.getActive() && u.getRole().getName().equals(roleName));
    }

    @Test
    @DisplayName("Should create new user")
    @DirtiesContext
    void shouldCreateNewUser() {
        // Given
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Customer role not found"));

        User newUser = User.builder()
                .email("newuser@quickbite.test")
                .passwordHash("$2a$10$hashedpassword")
                .name("New User")
                .phone("+91-9999999999")
                .role(customerRole)
                .active(true)
                .build();

        // When — saveAndFlush triggers @CreationTimestamp before assertions
        User savedUser = userRepository.saveAndFlush(newUser);
        // Refresh to load DB-generated values (createdAt, updatedAt) into the entity
        entityManager.refresh(savedUser);

        // Then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("newuser@quickbite.test");
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        // Verify we can find it
        Optional<User> foundUser = userRepository.findByEmail("newuser@quickbite.test");
        assertThat(foundUser).isPresent();
    }

    @Test
    @DisplayName("Should count active users by role")
    void shouldCountActiveUsersByRole() {
        // Given
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Customer role not found"));

        // When
        Long count = userRepository.countActiveUsersByRole(customerRole.getId());

        // Then
        assertThat(count).isGreaterThan(0L);
    }
}
