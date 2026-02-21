package com.quickbite.orders.controller;

import com.quickbite.auth.security.JwtTokenProvider;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.users.entity.Role;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.RoleRepository;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.MenuItemRepository;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrderController — full order lifecycle.
 * Phase 5 — covers CRUD, status transitions, vendor accept/reject.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("localtest")
@Transactional
class OrderControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private VendorRepository vendorRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private String vendorToken;
    private String driverToken;
    private String adminToken;
    private User customer;
    private User vendorUser;
    private User driverUser;
    private Vendor vendor;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        Role customerRole = roleRepository.findByName("CUSTOMER").orElseThrow();
        Role vendorRole = roleRepository.findByName("VENDOR").orElseThrow();
        Role driverRole = roleRepository.findByName("DRIVER").orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        customer = userRepository.save(User.builder()
                .email("oc-cust-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Order Customer")
                .role(customerRole)
                .active(true)
                .build());

        vendorUser = userRepository.save(User.builder()
                .email("oc-vendor-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Order Vendor")
                .role(vendorRole)
                .active(true)
                .build());

        driverUser = userRepository.save(User.builder()
                .email("oc-driver-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Order Driver")
                .role(driverRole)
                .active(true)
                .build());

        User adminUser = userRepository.save(User.builder()
                .email("oc-admin-" + UUID.randomUUID() + "@test.com")
                .passwordHash(passwordEncoder.encode("Pass@1234"))
                .name("Order Admin")
                .role(adminRole)
                .active(true)
                .build());

        vendor = vendorRepository.save(Vendor.builder()
                .user(vendorUser)
                .name("Order Test Restaurant")
                .active(true)
                .build());

        menuItem = menuItemRepository.save(MenuItem.builder()
                .vendor(vendor)
                .name("Test Pizza")
                .priceCents(19900L)
                .available(true)
                .prepTimeMins(20)
                .build());

        customerToken = jwtTokenProvider.generateAccessToken(customer.getId(), customer.getEmail(), "CUSTOMER");
        vendorToken = jwtTokenProvider.generateAccessToken(vendorUser.getId(), vendorUser.getEmail(), "VENDOR");
        driverToken = jwtTokenProvider.generateAccessToken(driverUser.getId(), driverUser.getEmail(), "DRIVER");
        adminToken = jwtTokenProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), "ADMIN");
    }

    private Order createTestOrder(OrderStatus status) {
        Order order = Order.builder()
                .customer(customer)
                .vendor(vendor)
                .subtotalCents(19900L)
                .deliveryFeeCents(500L)
                .taxCents(200L)
                .totalCents(20600L)
                .status(status)
                .build();
        order.addOrderItem(OrderItem.builder()
                .menuItem(menuItem)
                .quantity(1)
                .priceCents(19900L)
                .build());
        return orderRepository.save(order);
    }

    // ── GET /api/orders ──────────────────────────────────────────────

    @Test
    void listOrders_asCustomer_returnsOwnOrders() throws Exception {
        createTestOrder(OrderStatus.PLACED);

        mockMvc.perform(get("/api/orders?page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void listOrders_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/orders/{id} ─────────────────────────────────────────

    @Test
    void getOrder_byId_returnsDetail() throws Exception {
        Order order = createTestOrder(OrderStatus.PLACED);

        mockMvc.perform(get("/api/orders/" + order.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PLACED"));
    }

    // ── PATCH /api/orders/{id}/status ────────────────────────────────

    @Test
    void updateStatus_vendorPreparing_succeeds() throws Exception {
        Order order = createTestOrder(OrderStatus.ACCEPTED);

        Map<String, String> dto = Map.of("status", "PREPARING");

        mockMvc.perform(patch("/api/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));
    }

    // ── POST /api/orders/{id}/accept ─────────────────────────────────

    @Test
    void vendorAcceptOrder_succeeds() throws Exception {
        Order order = createTestOrder(OrderStatus.PLACED);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/accept")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    // ── POST /api/orders/{id}/reject ─────────────────────────────────

    @Test
    void vendorRejectOrder_cancels() throws Exception {
        Order order = createTestOrder(OrderStatus.PLACED);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/reject?reason=Out+of+stock")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // ── GET /api/orders/{id}/status-history ──────────────────────────

    @Test
    void getStatusHistory_returnsAuditTrail() throws Exception {
        Order order = createTestOrder(OrderStatus.PLACED);

        mockMvc.perform(get("/api/orders/" + order.getId() + "/status-history")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ── POST /api/orders/{id}/assign/{driverId} ─────────────────────

    @Test
    void assignDriver_asAdmin_succeeds() throws Exception {
        Order order = createTestOrder(OrderStatus.READY);

        mockMvc.perform(post("/api/orders/" + order.getId() + "/assign/" + driverUser.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ASSIGNED"));
    }
}
