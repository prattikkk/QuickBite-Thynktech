package com.quickbite.users.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for UserAccountController — profile, export, delete.
 * Phase 5 — API Completeness.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class UserAccountControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private User customer;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        customer = userRepository.save(User.builder()
                .email("profile-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Profile Test User")
                .phone("555-1234")
                .role(customerRole)
                .active(true)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");
    }

    @Test
    void getProfile_returnsCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(customer.getEmail()))
                .andExpect(jsonPath("$.data.name").value("Profile Test User"))
                .andExpect(jsonPath("$.data.phone").value("555-1234"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    void getProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfile_changesNameAndPhone() throws Exception {
        Map<String, String> dto = Map.of("name", "Updated Name", "phone", "555-9999");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.phone").value("555-9999"));
    }

    @Test
    void updateProfile_nullFieldsAreIgnored() throws Exception {
        // Only send name — phone should remain unchanged
        Map<String, String> dto = Map.of("name", "Only Name Changed");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Only Name Changed"))
                .andExpect(jsonPath("$.data.phone").value("555-1234"));
    }

    @Test
    void exportData_returnsStructuredPayload() throws Exception {
        mockMvc.perform(get("/api/users/me/export")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile").exists())
                .andExpect(jsonPath("$.data.addresses").isArray())
                .andExpect(jsonPath("$.data.orders").isArray())
                .andExpect(jsonPath("$.data.favorites").isArray())
                .andExpect(jsonPath("$.data.exportedAt").exists());
    }

    @Test
    void deleteAccount_anonymizesUser() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted and PII anonymized"));

        // Verify anonymization
        User deleted = userRepository.findById(customer.getId()).orElseThrow();
        assert deleted.getName().equals("Deleted User");
        assert deleted.getPhone() == null;
        assert !deleted.getActive();
    }
}
