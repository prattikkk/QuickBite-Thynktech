package com.quickbite.auth.controller;

import com.quickbite.auth.dto.AuthResponse;
import com.quickbite.auth.dto.LoginRequest;
import com.quickbite.auth.dto.RefreshRequest;
import com.quickbite.auth.dto.RegisterRequest;
import com.quickbite.auth.repository.TokenStoreRepository;
import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.common.dto.ApiResponse;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for authentication endpoints.
 * Uses Testcontainers for PostgreSQL database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private TokenStoreRepository tokenStoreRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Role customerRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test data
        // Note: @Transactional on the test class means each test gets its own
        // transaction that is rolled back automatically — no manual cleanup needed.
        customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("CUSTOMER role not found"));

        testUser = User.builder()
                .email("test@quickbite.test")
                .passwordHash(passwordEncoder.encode("Password@123"))
                .name("Test User")
                .phone("+91-1234567890")
                .role(customerRole)
                .active(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testRegisterNewUser_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@quickbite.test")
                .password("Password@123")
                .name("New User")
                .phone("+91-9876543210")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value("newuser@quickbite.test"))
                .andExpect(jsonPath("$.data.name").value("New User"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    void testRegisterDuplicateEmail_Fails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@quickbite.test") // Already exists
                .password("Password@123")
                .name("Duplicate User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("already registered")));
    }

    @Test
    void testRegisterInvalidPassword_Fails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("weak@quickbite.test")
                .password("weak") // Too short, no uppercase, no special char
                .name("Weak Password User")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void testLogin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("test@quickbite.test"));
    }

    @Test
    void testLoginInvalidCredentials_Fails() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("WrongPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testAccessProtectedEndpointWithoutToken_Fails() throws Exception {
        mockMvc.perform(get("/api/vendors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccessProtectedEndpointWithValidToken_Success() throws Exception {
        // Generate token
        String accessToken = jwtTokenProvider.generateAccessToken(
                testUser.getId(),
                testUser.getEmail(),
                testUser.getRole().getName()
        );

        mockMvc.perform(get("/actuator/health")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        // Login first to get tokens
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("Password@123")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String refreshToken = authResponse.getData().getRefreshToken();

        // Now test refresh
        RefreshRequest refreshRequest = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void testLogout_Success() throws Exception {
        // Login first
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("Password@123")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApiResponse<AuthResponse> authResponse = objectMapper.readValue(
                loginResponse,
                objectMapper.getTypeFactory().constructParametricType(
                        ApiResponse.class,
                        AuthResponse.class
                )
        );

        String refreshToken = authResponse.getData().getRefreshToken();

        // Logout
        RefreshRequest logoutRequest = RefreshRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Try to refresh with revoked token - should fail
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccountLockoutAfterFailedAttempts() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("WrongPassword@123")
                .build();

        // Make 4 failed attempts - should not lock yet
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid email or password")));
        }

        // 5th failed attempt should lock the account
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("locked")));

        // Even with correct password, should still be locked
        LoginRequest correctRequest = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("locked")));
    }

    @Test
    void testFailedAttemptsResetOnSuccessfulLogin() throws Exception {
        LoginRequest wrongRequest = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("WrongPassword@123")
                .build();

        // Make 3 failed attempts
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // Successful login should reset counter
        LoginRequest correctRequest = LoginRequest.builder()
                .email("test@quickbite.test")
                .password("Password@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(correctRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Now make 4 more failed attempts - they should start from 0 again
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(containsString("Invalid email or password")));
        }
    }
}
