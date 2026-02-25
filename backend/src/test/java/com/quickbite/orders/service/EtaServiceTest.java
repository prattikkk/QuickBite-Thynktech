package com.quickbite.orders.service;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.users.entity.Address;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.maps.service.HaversineMapsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for EtaService — Phase 3 ETA calculation.
 */
class EtaServiceTest {

    private EtaService etaService;

    private Vendor vendor;
    private Address vendorAddress;
    private Address deliveryAddress;

    @BeforeEach
    void setUp() {
        etaService = new EtaService(new HaversineMapsService());

        // Bangalore coordinates for vendor
        vendor = Vendor.builder()
                .id(UUID.randomUUID())
                .name("Test Restaurant")
                .lat(BigDecimal.valueOf(12.9716))
                .lng(BigDecimal.valueOf(77.5946))
                .build();

        // Delivery address ~5km away
        deliveryAddress = Address.builder()
                .id(UUID.randomUUID())
                .line1("456 Customer St")
                .lat(BigDecimal.valueOf(12.9350))
                .lng(BigDecimal.valueOf(77.6150))
                .build();
    }

    @Test
    @DisplayName("estimatePrepTime — uses max item prep time")
    void estimatePrepTime_success() {
        MenuItem item1 = MenuItem.builder()
                .prepTimeMins(10)
                .vendor(vendor)
                .build();
        MenuItem item2 = MenuItem.builder()
                .prepTimeMins(25)
                .vendor(vendor)
                .build();

        OrderItem oi1 = OrderItem.builder().menuItem(item1).quantity(2).build();
        OrderItem oi2 = OrderItem.builder().menuItem(item2).quantity(1).build();

        Order order = Order.builder()
                .vendor(vendor)
                .orderItems(List.of(oi1, oi2))
                .build();

        int prepTime = etaService.estimatePrepTime(order);

        assertThat(prepTime).isEqualTo(25); // max of 10, 25
    }

    @Test
    @DisplayName("estimatePrepTime — defaults to 15 when items have no prep time")
    void estimatePrepTime_defaultFifteen() {
        MenuItem item = MenuItem.builder()
                .prepTimeMins(null)
                .vendor(vendor)
                .build();
        OrderItem oi = OrderItem.builder().menuItem(item).quantity(1).build();

        Order order = Order.builder()
                .vendor(vendor)
                .orderItems(List.of(oi))
                .build();

        int prepTime = etaService.estimatePrepTime(order);

        assertThat(prepTime).isEqualTo(15); // default
    }

    @Test
    @DisplayName("estimateDelivery — returns future time")
    void estimateDelivery_returnsFutureTime() {
        MenuItem item = MenuItem.builder()
                .prepTimeMins(20)
                .vendor(vendor)
                .build();
        OrderItem oi = OrderItem.builder().menuItem(item).quantity(1).build();

        Order order = Order.builder()
                .vendor(vendor)
                .deliveryAddress(deliveryAddress)
                .orderItems(List.of(oi))
                .build();

        OffsetDateTime eta = etaService.estimateDelivery(order);

        assertThat(eta).isAfter(OffsetDateTime.now());
        // Should be at least prepTime + overhead = 20 + 5 = 25 minutes from now
        assertThat(eta).isAfter(OffsetDateTime.now().plusMinutes(20));
    }

    @Test
    @DisplayName("estimateDelivery — without coordinates uses fallback travel time")
    void estimateDelivery_noCoordinates_usesFallback() {
        Address noCoords = Address.builder()
                .id(UUID.randomUUID())
                .line1("No coords")
                .lat(null)
                .lng(null)
                .build();

        Vendor noCoordVendor = Vendor.builder()
                .id(UUID.randomUUID())
                .name("No Coord Restaurant")
                .lat(null)
                .lng(null)
                .build();

        MenuItem item = MenuItem.builder()
                .prepTimeMins(15)
                .vendor(noCoordVendor)
                .build();
        OrderItem oi = OrderItem.builder().menuItem(item).quantity(1).build();

        Order order = Order.builder()
                .vendor(noCoordVendor)
                .deliveryAddress(noCoords)
                .orderItems(List.of(oi))
                .build();

        OffsetDateTime eta = etaService.estimateDelivery(order);

        assertThat(eta).isAfter(OffsetDateTime.now());
        // prepTime(15) + travelFallback(20) + overhead(5) = 40 min
        assertThat(eta).isBefore(OffsetDateTime.now().plusMinutes(45));
    }
}
