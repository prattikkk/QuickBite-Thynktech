package com.quickbite.vendors.service;

import com.quickbite.reviews.repository.ReviewRepository;
import com.quickbite.vendors.dto.VendorResponseDTO;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service layer for vendor queries with Redis caching.
 * Caching lives here (not in controller) so that only serialisable
 * DTOs / Maps are stored — never ResponseEntity.
 */
@Service
@RequiredArgsConstructor
public class VendorCacheService {

    private final VendorRepository vendorRepository;
    private final ReviewRepository reviewRepository;

    @Cacheable(value = "vendors", key = "#page + '-' + #size")
    public Map<String, Object> listActiveVendors(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Vendor> vendorPage = vendorRepository.findByActiveTrue(pageable);
        return toPageMap(vendorPage);
    }

    @Cacheable(value = "vendorById", key = "#id")
    public VendorResponseDTO getVendorById(UUID id) {
        var vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + id));
        return toDTO(vendor);
    }

    @Cacheable(value = "vendorSearch", key = "#query + '-' + #page + '-' + #size")
    public Map<String, Object> searchVendors(String query, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<Vendor> vendorPage = vendorRepository.findByNameContainingIgnoreCaseAndActiveTrue(query, pageable);
        return toPageMap(vendorPage);
    }

    @CacheEvict(value = {"vendors", "vendorSearch"}, allEntries = true)
    public void evictVendorListCaches() {
        // intentionally empty — annotation does the work
    }

    @CacheEvict(value = {"vendors", "vendorById", "vendorSearch"}, allEntries = true)
    public void evictAllVendorCaches() {
        // intentionally empty — annotation does the work
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Map<String, Object> toPageMap(Page<Vendor> vendorPage) {
        List<VendorResponseDTO> content = vendorPage.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("content", content);
        map.put("page", vendorPage.getNumber());
        map.put("size", vendorPage.getSize());
        map.put("totalElements", vendorPage.getTotalElements());
        map.put("totalPages", vendorPage.getTotalPages());
        return map;
    }

    public VendorResponseDTO toDTO(Vendor vendor) {
        return VendorResponseDTO.builder()
                .id(vendor.getId())
                .userId(vendor.getUser() != null ? vendor.getUser().getId() : null)
                .name(vendor.getName())
                .description(vendor.getDescription())
                .address(vendor.getAddress())
                .lat(vendor.getLat())
                .lng(vendor.getLng())
                .openHours(vendor.getOpenHours())
                .rating(vendor.getRating())
                .reviewCount(reviewRepository.countByVendorIdAndHiddenFalse(vendor.getId()))
                .deliveryRadiusKm(vendor.getDeliveryRadiusKm())
                .active(vendor.getActive())
                .menuItemCount(vendor.getMenuItems() != null ? vendor.getMenuItems().size() : 0)
                .createdAt(vendor.getCreatedAt())
                .updatedAt(vendor.getUpdatedAt())
                .build();
    }
}
