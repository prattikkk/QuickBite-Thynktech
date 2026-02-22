package com.quickbite.vendors.service;

import com.quickbite.vendors.dto.MenuItemResponseDTO;
import com.quickbite.vendors.entity.MenuItem;
import com.quickbite.vendors.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for menu-item queries with Redis caching.
 */
@Service
@RequiredArgsConstructor
public class MenuItemCacheService {

    private final MenuItemRepository menuItemRepository;

    @Cacheable(value = "menus", key = "#vendorId + '-' + #includeUnavailable")
    public List<MenuItemResponseDTO> getVendorMenu(UUID vendorId, boolean includeUnavailable) {
        List<MenuItem> items = includeUnavailable
                ? menuItemRepository.findByVendorId(vendorId)
                : menuItemRepository.findByVendorIdAndAvailableTrue(vendorId);
        return items.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @CacheEvict(value = "menus", allEntries = true)
    public void evictMenuCaches() {
        // intentionally empty — annotation does the work
    }

    public MenuItemResponseDTO toDTO(MenuItem item) {
        return MenuItemResponseDTO.builder()
                .id(item.getId())
                .vendorId(item.getVendor() != null ? item.getVendor().getId() : null)
                .name(item.getName())
                .description(item.getDescription())
                .priceCents(item.getPriceCents())
                .price(item.getPriceCents() != null ? item.getPriceCents() / 100.0 : null)
                .available(item.getAvailable())
                .prepTimeMins(item.getPrepTimeMins())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
