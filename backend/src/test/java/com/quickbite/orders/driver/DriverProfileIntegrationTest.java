package com.quickbite.orders.driver;

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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Phase 1 Driver Profile endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class DriverProfileIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private DriverProfileRepository driverProfileRepository;

    private String driverToken;
    private String customerToken;
    private User driver;

    @BeforeEach
    void setUp() {
        Role driverRole = roleRepository.findByName("DRIVER").orElseThrow();
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();

        driver = userRepository.save(User.builder()
                .email("driver-profile-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Profile Test Driver")
                .role(driverRole)
                .active(true)
                .build());

        User customer = userRepository.save(User.builder()
                .email("cust-profile-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Customer")
                .role(customerRole)
                .active(true)
                .build());

        driverToken = jwtTokenProvider.generateAccessToken(driver.getId(), driver.getEmail(), "DRIVER");
        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");
    }

    // ── GET /api/drivers/profile ─────────────────────────────────────

    @Test
    void getProfile_createsProfileOnFirstAccess() throws Exception {
        mockMvc.perform(get("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(driver.getId().toString()))
                .andExpect(jsonPath("$.data.name").value("Profile Test Driver"))
                .andExpect(jsonPath("$.data.isOnline").value(false))
                .andExpect(jsonPath("$.data.totalDeliveries").value(0))
                .andExpect(jsonPath("$.data.successRate").value(100.0));
    }

    @Test
    void getProfile_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/drivers/profile")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProfile_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/drivers/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/drivers/profile ─────────────────────────────────────

    @Test
    void updateProfile_setsVehicleAndPlate() throws Exception {
        Map<String, String> body = Map.of(
                "vehicleType", "MOTORCYCLE",
                "licensePlate", "AB-1234"
        );

        mockMvc.perform(put("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"))
                .andExpect(jsonPath("$.data.licensePlate").value("AB-1234"));

        // Verify persisted
        mockMvc.perform(get("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(jsonPath("$.data.vehicleType").value("MOTORCYCLE"))
                .andExpect(jsonPath("$.data.licensePlate").value("AB-1234"));
    }

    @Test
    void updateProfile_customerRole_returns403() throws Exception {
        mockMvc.perform(put("/api/drivers/profile")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"vehicleType\":\"CAR\",\"licensePlate\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/drivers/status ──────────────────────────────────────

    @Test
    void toggleStatus_goOnline() throws Exception {
        mockMvc.perform(put("/api/drivers/status")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"online\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isOnline").value(true));
    }

    @Test
    void toggleStatus_goOffline() throws Exception {
        // First go online
        mockMvc.perform(put("/api/drivers/status")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"online\":true}"))
                .andExpect(status().isOk());

        // Then go offline
        mockMvc.perform(put("/api/drivers/status")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"online\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isOnline").value(false));
    }

    @Test
    void toggleStatus_customerRole_returns403() throws Exception {
        mockMvc.perform(put("/api/drivers/status")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"online\":true}"))
                .andExpect(status().isForbidden());
    }

    // ── Profile + location integration ───────────────────────────────

    @Test
    void locationUpdate_alsoUpdatesProfile() throws Exception {
        // Ensure profile exists first
        mockMvc.perform(get("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk());

        // Update location
        Map<String, Object> loc = Map.of("lat", 40.7128, "lng", -74.0060);
        mockMvc.perform(put("/api/drivers/location")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc)))
                .andExpect(status().isOk());

        // Verify profile has location
        mockMvc.perform(get("/api/drivers/profile")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(jsonPath("$.data.currentLat").value(closeTo(40.7128, 0.001)))
                .andExpect(jsonPath("$.data.currentLng").value(closeTo(-74.006, 0.001)));
    }
}
