package com.quickbite.common.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminManagementController — user/vendor governance.
 * Phase 5 — Admin moderation endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class AdminManagementControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String customerToken;
    private User customerUser;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();

        User admin = userRepository.save(User.builder()
                .email("admin-mgmt-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Admin")
                .role(adminRole)
                .active(true)
                .build());

        customerUser = userRepository.save(User.builder()
                .email("mgmt-cust-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Manageable Customer")
                .phone("555-0000")
                .role(customerRole)
                .active(true)
                .build());

        User vendorUser = userRepository.save(User.builder()
                .email("mgmt-vendor-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Vendor Owner")
                .role(vendorRole)
                .active(true)
                .build());

        testVendor = vendorRepository.save(Vendor.builder()
                .user(vendorUser)
                .name("Pending Restaurant")
                .active(false)
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");
        customerToken = jwtTokenProvider.generateAccessToken(customerUser.getId(), customerUser.getEmail(), "CUSTOMER");
    }

    // ── User management ──────────────────────────────────────────────

    @Test
    void listUsers_asAdmin_succeeds() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void listUsers_asCustomer_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users?page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void banUser_setsInactive() throws Exception {
        mockMvc.perform(put("/api/admin/users/" + customerUser.getId() + "/status?active=false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));

        // Verify in DB
        User banned = userRepository.findById(customerUser.getId()).orElseThrow();
        assert !banned.getActive();
    }

    @Test
    void activateUser_setsActive() throws Exception {
        // First ban
        customerUser.setActive(false);
        userRepository.save(customerUser);

        mockMvc.perform(put("/api/admin/users/" + customerUser.getId() + "/status?active=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    // ── Vendor management ────────────────────────────────────────────

    @Test
    void listVendors_asAdmin_includesInactive() throws Exception {
        mockMvc.perform(get("/api/admin/vendors?page=0&size=10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void approveVendor_activatesIt() throws Exception {
        mockMvc.perform(put("/api/admin/vendors/" + testVendor.getId() + "/approve?active=true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void deactivateVendor_setsInactive() throws Exception {
        testVendor.setActive(true);
        vendorRepository.save(testVendor);

        mockMvc.perform(put("/api/admin/vendors/" + testVendor.getId() + "/approve?active=false")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    @Test
    void noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
