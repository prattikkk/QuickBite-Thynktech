package com.quickbite.favorites.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
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
 * Integration tests for FavoriteController.
 * Phase 4 — Testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class FavoriteControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();

        User customer = userRepository.save(User.builder()
                .email("fav-cust-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Fav Customer")
                .role(customerRole)
                .active(true)
                .build());

        User vendorUser = userRepository.save(User.builder()
                .email("fav-vendor-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Fav Vendor")
                .role(vendorRole)
                .active(true)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");

        testVendor = vendorRepository.save(Vendor.builder()
                .user(vendorUser)
                .name("Favorite Test Restaurant")
                .description("Yummy food")
                .active(true)
                .build());
    }

    @Test
    void addFavorite_thenList_thenRemove() throws Exception {
        // Add to favorites
        mockMvc.perform(post("/api/favorites/" + testVendor.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // List favorites — should contain vendor
        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].vendorName").value("Favorite Test Restaurant"));

        // Check is-favorite
        mockMvc.perform(get("/api/favorites/" + testVendor.getId() + "/check")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        // Remove from favorites
        mockMvc.perform(delete("/api/favorites/" + testVendor.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // List favorites — should be empty
        mockMvc.perform(get("/api/favorites")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
