package com.quickbite.orders.repository;

import com.quickbite.BaseIntegrationTest;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.orders.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderRepository using Testcontainers.
 * Tests order queries, status filtering, and relationships.
 */
@SpringBootTest
@Transactional
class OrderRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    @DisplayName("Should find orders by customer ID")
    void shouldFindOrdersByCustomerId() {
        // Given: Alice's user ID from sample data
        UUID customerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        // When
        Page<Order> orders = orderRepository.findByCustomerId(customerId, PageRequest.of(0, 10));

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders.getContent()).allMatch(o -> o.getCustomer().getId().equals(customerId));
    }

    @Test
    @DisplayName("Should find orders by vendor ID")
    void shouldFindOrdersByVendorId() {
        // Given: Tasty Burger Joint vendor ID
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        Page<Order> orders = orderRepository.findByVendorId(vendorId, PageRequest.of(0, 10));

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders.getContent()).allMatch(o -> o.getVendor().getId().equals(vendorId));
    }

    @Test
    @DisplayName("Should find orders by status")
    void shouldFindOrdersByStatus() {
        // Given
        OrderStatus status = OrderStatus.DELIVERED; // migrated from COMPLETED

        // When
        Page<Order> orders = orderRepository.findByStatus(status, PageRequest.of(0, 10));

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders.getContent()).allMatch(o -> o.getStatus() == status);
    }

    @Test
    @DisplayName("Should find orders by vendor and status")
    void shouldFindOrdersByVendorAndStatus() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");
        OrderStatus status = OrderStatus.DELIVERED; // migrated from COMPLETED

        // When
        List<Order> orders = orderRepository.findByVendorIdAndStatus(vendorId, status);

        // Then
        assertThat(orders).isNotEmpty();
        assertThat(orders).allMatch(o ->
                o.getVendor().getId().equals(vendorId) && o.getStatus() == status
        );
    }

    @Test
    @DisplayName("Should count orders by customer")
    void shouldCountOrdersByCustomer() {
        // Given
        UUID customerId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        // When
        Long count = orderRepository.countByCustomerId(customerId);

        // Then
        assertThat(count).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Should verify order with items")
    void shouldVerifyOrderWithItems() {
        // Given: Sample order ID
        UUID orderId = UUID.fromString("20000001-0000-0000-0000-000000000001");

        // When
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        // Then
        assertThat(orderOpt).isPresent();
        Order order = orderOpt.get();

        assertThat(order.getCustomer()).isNotNull();
        assertThat(order.getVendor()).isNotNull();
        assertThat(order.getTotalCents()).isGreaterThan(0L);
        assertThat(order.getStatus()).isNotNull();
        assertThat(order.getCreatedAt()).isNotNull();

        // Verify order items
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        assertThat(items).isNotEmpty();
        assertThat(items).hasSize(2); // Sample order has 2 items

        // Verify total calculation
        Long calculatedTotal = items.stream()
                .mapToLong(OrderItem::calculateTotal)
                .sum();
        assertThat(calculatedTotal).isEqualTo(order.getTotalCents());
    }

    @Test
    @DisplayName("Should find unassigned orders")
    void shouldFindUnassignedOrders() {
        // Given: A status that might have unassigned orders
        OrderStatus status = OrderStatus.READY;

        // When
        List<Order> unassignedOrders = orderRepository.findByDriverIsNullAndStatus(status);

        // Then: Should find orders without drivers (or be empty if all assigned)
        assertThat(unassignedOrders).allMatch(o -> o.getDriver() == null);
    }

    @Test
    @DisplayName("Should verify order status constants")
    void shouldVerifyOrderStatusConstants() {
        // Verify the OrderStatus enum values exist
        assertThat(OrderStatus.PLACED.name()).isEqualTo("PLACED");
        assertThat(OrderStatus.ACCEPTED.name()).isEqualTo("ACCEPTED");
        assertThat(OrderStatus.PREPARING.name()).isEqualTo("PREPARING");
        assertThat(OrderStatus.READY.name()).isEqualTo("READY");
        assertThat(OrderStatus.ASSIGNED.name()).isEqualTo("ASSIGNED");
        assertThat(OrderStatus.PICKED_UP.name()).isEqualTo("PICKED_UP");
        assertThat(OrderStatus.ENROUTE.name()).isEqualTo("ENROUTE");
        assertThat(OrderStatus.DELIVERED.name()).isEqualTo("DELIVERED");
        assertThat(OrderStatus.CANCELLED.name()).isEqualTo("CANCELLED");
    }
}
