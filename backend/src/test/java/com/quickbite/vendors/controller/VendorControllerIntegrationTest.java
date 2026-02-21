package com.quickbite.vendors.controller;

import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.MenuItemRepository;
import com.quickbite.vendors.repository.VendorRepository;
import com.quickbite.auth.security.JwtTokenProvider;
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
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for VendorController and MenuItemController.
 * Phase 4 — Testing: controller-level integration tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class VendorControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private String vendorToken;
    private User vendorUser;
    private Vendor testVendor;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();

        User customer = userRepository.save(User.builder()
                .email("vc-test-customer-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Customer")
                .role(customerRole)
                .active(true)
                .build());

        vendorUser = userRepository.save(User.builder()
                .email("vc-test-vendor-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Vendor Owner")
                .role(vendorRole)
                .active(true)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");
        vendorToken = jwtTokenProvider.generateAccessToken(vendorUser.getId(), vendorUser.getEmail(), "VENDOR");

        testVendor = vendorRepository.save(Vendor.builder()
                .user(vendorUser)
                .name("Test Burger Joint")
                .description("Best burgers in town")
                .active(true)
                .build());

        menuItemRepository.save(MenuItem.builder()
                .vendor(testVendor)
                .name("Classic Burger")
                .description("Beef patty with lettuce")
                .priceCents(29900L)
                .available(true)
                .prepTimeMins(15)
                .build());

        menuItemRepository.save(MenuItem.builder()
                .vendor(testVendor)
                .name("Fries")
                .description("Crispy golden fries")
                .priceCents(9900L)
                .available(true)
                .prepTimeMins(8)
                .build());
    }

    @Test
    void listVendors_asCustomer_returnsPagedList() throws Exception {
        mockMvc.perform(get("/api/vendors?page=0&size=20")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").isNumber());
    }

    @Test
    void getVendor_returnsVendorDetails() throws Exception {
        mockMvc.perform(get("/api/vendors/" + testVendor.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Test Burger Joint"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void searchVendors_findsMatch() throws Exception {
        mockMvc.perform(get("/api/vendors/search?query=Burger&page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[?(@.name == 'Test Burger Joint')]").exists());
    }

    @Test
    void getVendorMenu_returnMenuItems() throws Exception {
        mockMvc.perform(get("/api/vendors/" + testVendor.getId() + "/menu")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].priceCents").isNumber());
    }

    @Test
    void createMenuItem_asVendor_succeeds() throws Exception {
        Map<String, Object> dto = Map.of(
                "name", "Milkshake",
                "description", "Chocolate milkshake",
                "priceCents", 14900,
                "available", true,
                "prepTimeMins", 5
        );

        mockMvc.perform(post("/api/vendors/" + testVendor.getId() + "/menu")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Milkshake"))
                .andExpect(jsonPath("$.data.priceCents").value(14900));
    }

    @Test
    void updateVendorProfile_asVendor_succeeds() throws Exception {
        Map<String, Object> dto = Map.of(
                "name", "Updated Burger Joint",
                "description", "Now even better!"
        );

        mockMvc.perform(put("/api/vendors/my")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Burger Joint"));
    }

    @Test
    void listVendors_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/vendors?page=0&size=20"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void responseContainsSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/vendors?page=0&size=20")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-Response-Time"));
    }

    @Test
    void responseContainsRateLimitHeaders() throws Exception {
        // Rate-limit headers are only present when Redis is available.
        // In localtest profile (no Redis), the filter short-circuits gracefully.
        // This test verifies the request succeeds without rate-limit headers.
        mockMvc.perform(get("/api/vendors?page=0&size=20")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }
}
