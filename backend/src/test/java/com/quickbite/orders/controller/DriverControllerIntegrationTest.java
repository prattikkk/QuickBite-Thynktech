package com.quickbite.orders.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
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
 * Integration tests for DriverController.
 * Phase 5 — Driver-specific REST endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class DriverControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String driverToken;
    private String customerToken;
    private User driver;
    private User customerUser;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        Role driverRole = roleRepository.findByName("DRIVER").orElseThrow();
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();

        driver = userRepository.save(User.builder()
                .email("driver-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Driver")
                .role(driverRole)
                .active(true)
                .build());

        customerUser = userRepository.save(User.builder()
                .email("cust-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Customer")
                .role(customerRole)
                .active(true)
                .build());

        User vendorUser = userRepository.save(User.builder()
                .email("vendor-test-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Test Vendor Owner")
                .role(vendorRole)
                .active(true)
                .build());

        vendor = vendorRepository.save(Vendor.builder()
                .user(vendorUser)
                .name("Test Pizzeria")
                .active(true)
                .build());

        driverToken = jwtTokenProvider.generateAccessToken(driver.getId(), driver.getEmail(), "DRIVER");
        customerToken = jwtTokenProvider.generateAccessToken(customerUser.getId(), customerUser.getEmail(), "CUSTOMER");
    }

    private Order createOrder(OrderStatus status) {
        return orderRepository.save(Order.builder()
                .customer(customerUser)
                .vendor(vendor)
                .subtotalCents(1000L)
                .deliveryFeeCents(200L)
                .taxCents(100L)
                .totalCents(1300L)
                .status(status)
                .build());
    }

    @Test
    void getAvailableOrders_driverRole_succeeds() throws Exception {
        createOrder(OrderStatus.READY);

        mockMvc.perform(get("/api/drivers/available-orders")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].orderId").exists())
                .andExpect(jsonPath("$.data[0].status").value("READY"));
    }

    @Test
    void getAvailableOrders_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/drivers/available-orders")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveDelivery_noActive_returnsNull() throws Exception {
        mockMvc.perform(get("/api/drivers/active-delivery")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getActiveDelivery_withAssigned_returnsOrder() throws Exception {
        Order order = createOrder(OrderStatus.ASSIGNED);
        order.setDriver(driver);
        orderRepository.save(order);

        mockMvc.perform(get("/api/drivers/active-delivery")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(order.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }

    @Test
    void updateLocation_withActiveOrder_succeeds() throws Exception {
        Order order = createOrder(OrderStatus.ENROUTE);
        order.setDriver(driver);
        orderRepository.save(order);

        Map<String, Object> loc = Map.of("lat", 40.7128, "lng", -74.0060);

        mockMvc.perform(put("/api/drivers/location")
                        .header("Authorization", "Bearer " + driverToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getDeliveryHistory_returnsPagedList() throws Exception {
        Order order = createOrder(OrderStatus.DELIVERED);
        order.setDriver(driver);
        orderRepository.save(order);

        mockMvc.perform(get("/api/drivers/delivery-history?page=0&size=10")
                        .header("Authorization", "Bearer " + driverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/drivers/available-orders"))
                .andExpect(status().isUnauthorized());
    }
}
