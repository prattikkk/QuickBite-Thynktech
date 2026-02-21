package com.quickbite.promotions.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.promotions.dto.PromoCreateRequest;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
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

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PromoCodeController.
 * Phase 4 — Testing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class PromoCodeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        User admin = userRepository.save(User.builder()
                .email("promo-admin-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Promo Admin")
                .role(adminRole)
                .active(true)
                .build());

        User customer = userRepository.save(User.builder()
                .email("promo-cust-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Promo Customer")
                .role(customerRole)
                .active(true)
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getEmail(), "ADMIN");
        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");
    }

    @Test
    void adminCRUD_promoCode() throws Exception {
        String code = "TEST" + System.currentTimeMillis();
        PromoCreateRequest req = new PromoCreateRequest();
        req.setCode(code);
        req.setDescription("10% off everything");
        req.setDiscountType("PERCENT");
        req.setDiscountValue(1000L);  // 10% in basis points
        req.setMinOrderCents(5000L);
        req.setMaxDiscountCents(2000L);
        req.setMaxUses(100);
        req.setValidFrom(OffsetDateTime.now().minusDays(1));
        req.setValidUntil(OffsetDateTime.now().plusDays(30));
        req.setActive(true);

        // Create
        String createResp = mockMvc.perform(post("/api/promos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value(code))
                .andExpect(jsonPath("$.data.discountType").value("PERCENT"))
                .andReturn().getResponse().getContentAsString();

        String promoId = objectMapper.readTree(createResp).path("data").path("id").asText();

        // List
        mockMvc.perform(get("/api/promos")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        // Get by id
        mockMvc.perform(get("/api/promos/" + promoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code));

        // Update
        req.setDescription("Updated description");
        mockMvc.perform(put("/api/promos/" + promoId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated description"));

        // Delete
        mockMvc.perform(delete("/api/promos/" + promoId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void validatePromo_asCustomer() throws Exception {
        // First create a FIXED promo as admin
        String code = "FIXED" + System.currentTimeMillis();
        PromoCreateRequest req = new PromoCreateRequest();
        req.setCode(code);
        req.setDescription("$5 off");
        req.setDiscountType("FIXED");
        req.setDiscountValue(500L);  // 500 cents = $5
        req.setMinOrderCents(1000L);
        req.setActive(true);
        req.setValidFrom(OffsetDateTime.now().minusDays(1));
        req.setValidUntil(OffsetDateTime.now().plusDays(30));

        mockMvc.perform(post("/api/promos")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Validate as customer with sufficient subtotal
        mockMvc.perform(get("/api/promos/validate")
                        .param("code", code)
                        .param("subtotalCents", "5000")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.discountCents").value(500));
    }

    @Test
    void customerCannotCreatePromo() throws Exception {
        PromoCreateRequest req = new PromoCreateRequest();
        req.setCode("NOPE");
        req.setDiscountType("FIXED");
        req.setDiscountValue(100L);
        req.setActive(true);

        mockMvc.perform(post("/api/promos")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }
}
