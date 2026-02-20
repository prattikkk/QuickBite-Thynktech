package com.quickbite.vendors.repository;

import com.quickbite.BaseIntegrationTest;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.entity.Vendor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MenuItemRepository using Testcontainers.
 * Tests menu item queries and vendor relationships.
 */
@SpringBootTest
@Transactional
class MenuItemRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Test
    @DisplayName("Should find menu items by vendor ID")
    void shouldFindMenuItemsByVendorId() {
        // Given: Sample vendor from migration (Tasty Burger Joint)
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        List<MenuItem> menuItems = menuItemRepository.findByVendorId(vendorId);

        // Then
        assertThat(menuItems).isNotEmpty();
        assertThat(menuItems).hasSize(5); // 5 items per vendor in sample data
        assertThat(menuItems).allMatch(item -> item.getVendor().getId().equals(vendorId));
    }

    @Test
    @DisplayName("Should find available menu items only")
    void shouldFindAvailableMenuItems() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        List<MenuItem> availableItems = menuItemRepository.findByVendorIdAndAvailableTrue(vendorId);

        // Then
        assertThat(availableItems).isNotEmpty();
        assertThat(availableItems).allMatch(MenuItem::getAvailable);
    }

    @Test
    @DisplayName("Should find menu items by category")
    void shouldFindMenuItemsByCategory() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");
        String category = "Burgers";

        // When
        List<MenuItem> burgers = menuItemRepository.findByVendorIdAndCategory(vendorId, category);

        // Then
        assertThat(burgers).isNotEmpty();
        assertThat(burgers).allMatch(item -> item.getCategory().equals(category));
    }

    @Test
    @DisplayName("Should search menu items by name")
    void shouldSearchMenuItemsByName() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");
        String searchTerm = "burger";

        // When
        List<MenuItem> results = menuItemRepository.searchMenuItems(searchTerm, vendorId);

        // Then
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(item ->
                item.getName().toLowerCase().contains(searchTerm.toLowerCase())
        );
    }

    @Test
    @DisplayName("Should find distinct categories for vendor")
    void shouldFindDistinctCategories() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        List<String> categories = menuItemRepository.findCategoriesByVendorId(vendorId);

        // Then
        assertThat(categories).isNotEmpty();
        assertThat(categories).containsExactlyInAnyOrder("Burgers", "Sides", "Beverages");
    }

    @Test
    @DisplayName("Should count available menu items")
    void shouldCountAvailableMenuItems() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        Long count = menuItemRepository.countByVendorIdAndAvailableTrue(vendorId);

        // Then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should find menu items within price range")
    void shouldFindMenuItemsByPriceRange() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");
        Long minPrice = 10000L; // ₹100.00
        Long maxPrice = 25000L; // ₹250.00

        // When
        List<MenuItem> items = menuItemRepository.findByPriceRange(vendorId, minPrice, maxPrice);

        // Then
        assertThat(items).isNotEmpty();
        assertThat(items).allMatch(item ->
                item.getPriceCents() >= minPrice && item.getPriceCents() <= maxPrice
        );
    }

    @Test
    @DisplayName("Should verify menu item properties")
    void shouldVerifyMenuItemProperties() {
        // Given
        UUID vendorId = UUID.fromString("10000001-0000-0000-0000-000000000001");

        // When
        List<MenuItem> items = menuItemRepository.findByVendorId(vendorId);
        MenuItem firstItem = items.get(0);

        // Then
        assertThat(firstItem.getId()).isNotNull();
        assertThat(firstItem.getName()).isNotBlank();
        assertThat(firstItem.getPriceCents()).isGreaterThan(0L);
        assertThat(firstItem.getPrepTimeMins()).isGreaterThan(0);
        assertThat(firstItem.getCreatedAt()).isNotNull();
        assertThat(firstItem.getUpdatedAt()).isNotNull();
    }
}
