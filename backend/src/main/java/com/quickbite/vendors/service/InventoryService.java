package com.quickbite.vendors.service;

import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.service.NotificationService;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.vendors.dto.MenuItemResponseDTO;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing vendor inventory: stock levels, low-stock alerts,
 * auto-disable, and daily stock resets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MenuItemRepository menuItemRepository;
    private final NotificationService notificationService;

    // In-memory thresholds and stock counters (in production, persist these columns on MenuItem)
    private final Map<UUID, Integer> stockCounts = new LinkedHashMap<>();
    private final Map<UUID, Integer> lowStockThresholds = new LinkedHashMap<>();
    private final Map<UUID, Integer> dailyStockResets = new LinkedHashMap<>();
    private final Map<UUID, Boolean> autoDisableFlags = new LinkedHashMap<>();

    /**
     * Update stock count for a menu item.
     *
     * @param menuItemId the menu item UUID
     * @param stockCount new stock count
     * @return MenuItemResponseDTO with updated availability
     */
    @Transactional
    public MenuItemResponseDTO updateStock(UUID menuItemId, int stockCount) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new BusinessException("Menu item not found: " + menuItemId));

        stockCounts.put(menuItemId, stockCount);

        if (stockCount <= 0 && Boolean.TRUE.equals(autoDisableFlags.getOrDefault(menuItemId, true))) {
            item.setAvailable(false);
            menuItemRepository.save(item);
            log.info("Menu item {} auto-disabled due to zero stock", menuItemId);
        } else if (stockCount > 0 && !item.getAvailable()) {
            item.setAvailable(true);
            menuItemRepository.save(item);
        }

        checkLowStock(menuItemId, item);

        return toDTO(item);
    }

    /**
     * Decrement stock after an order is placed. Auto-disables the item if stock reaches 0.
     *
     * @param menuItemId the menu item UUID
     * @param quantity   quantity to decrement
     */
    @Transactional
    public void decrementStock(UUID menuItemId, int quantity) {
        int current = stockCounts.getOrDefault(menuItemId, Integer.MAX_VALUE);
        int newStock = Math.max(0, current - quantity);
        stockCounts.put(menuItemId, newStock);

        if (newStock <= 0) {
            MenuItem item = menuItemRepository.findById(menuItemId).orElse(null);
            if (item != null && Boolean.TRUE.equals(autoDisableFlags.getOrDefault(menuItemId, true))) {
                item.setAvailable(false);
                menuItemRepository.save(item);
                log.info("Menu item {} auto-disabled after stock decrement to 0", menuItemId);
            }
        }

        MenuItem item = menuItemRepository.findById(menuItemId).orElse(null);
        if (item != null) {
            checkLowStock(menuItemId, item);
        }
    }

    /**
     * Get inventory status for all items of a vendor.
     *
     * @param vendorId the vendor UUID
     * @return list of maps with itemId, name, stockCount, lowStock, available
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getInventoryStatus(UUID vendorId) {
        List<MenuItem> items = menuItemRepository.findByVendorId(vendorId);

        return items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId", item.getId());
            row.put("name", item.getName());
            int stock = stockCounts.getOrDefault(item.getId(), -1);
            row.put("stockCount", stock == -1 ? null : stock);
            int threshold = lowStockThresholds.getOrDefault(item.getId(), 5);
            row.put("lowStock", stock != -1 && stock <= threshold);
            row.put("available", item.getAvailable());
            return row;
        }).collect(Collectors.toList());
    }

    /**
     * Set the low-stock alert threshold for a menu item.
     *
     * @param menuItemId the menu item UUID
     * @param threshold  low-stock threshold value
     */
    public void setLowStockThreshold(UUID menuItemId, int threshold) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new BusinessException("Menu item not found: " + menuItemId);
        }
        lowStockThresholds.put(menuItemId, threshold);
        log.info("Low-stock threshold set to {} for item {}", threshold, menuItemId);
    }

    /**
     * Reset stock to the configured dailyStockReset value for all items of a vendor.
     *
     * @param vendorId the vendor UUID
     */
    @Transactional
    public void resetDailyStock(UUID vendorId) {
        List<MenuItem> items = menuItemRepository.findByVendorId(vendorId);

        for (MenuItem item : items) {
            Integer resetValue = dailyStockResets.get(item.getId());
            if (resetValue != null) {
                stockCounts.put(item.getId(), resetValue);
                if (resetValue > 0 && !item.getAvailable()) {
                    item.setAvailable(true);
                    menuItemRepository.save(item);
                }
            }
        }
        log.info("Daily stock reset completed for vendor {}", vendorId);
    }

    /**
     * Configure daily stock reset value for an item (used internally).
     */
    public void setDailyStockReset(UUID menuItemId, int resetValue) {
        dailyStockResets.put(menuItemId, resetValue);
    }

    /**
     * Configure auto-disable-on-zero flag for an item.
     */
    public void setAutoDisableOnZero(UUID menuItemId, boolean autoDisable) {
        autoDisableFlags.put(menuItemId, autoDisable);
    }

    // ---- private helpers ----

    private void checkLowStock(UUID menuItemId, MenuItem item) {
        int stock = stockCounts.getOrDefault(menuItemId, Integer.MAX_VALUE);
        int threshold = lowStockThresholds.getOrDefault(menuItemId, 5);
        if (stock <= threshold && stock > 0) {
            try {
                notificationService.createNotification(
                        item.getVendor().getUser().getId(),
                        NotificationType.ORDER_UPDATE,
                        "Low Stock Alert",
                        "Item '" + item.getName() + "' has only " + stock + " left in stock.",
                        item.getId()
                );
            } catch (Exception e) {
                log.warn("Failed to send low-stock notification for item {}: {}", menuItemId, e.getMessage());
            }
        }
    }

    private MenuItemResponseDTO toDTO(MenuItem item) {
        return MenuItemResponseDTO.builder()
                .id(item.getId())
                .vendorId(item.getVendor().getId())
                .name(item.getName())
                .description(item.getDescription())
                .priceCents(item.getPriceCents())
                .price(item.getPriceCents() / 100.0)
                .available(item.getAvailable())
                .prepTimeMins(item.getPrepTimeMins())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
