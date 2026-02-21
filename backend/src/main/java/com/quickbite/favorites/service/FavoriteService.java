package com.quickbite.favorites.service;

import com.quickbite.favorites.dto.FavoriteDTO;
import com.quickbite.favorites.entity.Favorite;
import com.quickbite.favorites.repository.FavoriteRepository;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing customer favorite vendors.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    /**
     * Add a vendor to the customer's favorites.
     */
    @Transactional
    public FavoriteDTO addFavorite(UUID userId, UUID vendorId) {
        if (favoriteRepository.existsByUserIdAndVendorId(userId, vendorId)) {
            throw new BusinessException("Vendor is already in your favorites");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BusinessException("Vendor not found: " + vendorId));

        Favorite favorite = Favorite.builder()
                .user(user)
                .vendor(vendor)
                .build();

        favorite = favoriteRepository.save(favorite);
        log.info("User {} added vendor {} to favorites", userId, vendorId);
        return toDTO(favorite);
    }

    /**
     * Remove a vendor from the customer's favorites.
     */
    @Transactional
    public void removeFavorite(UUID userId, UUID vendorId) {
        if (!favoriteRepository.existsByUserIdAndVendorId(userId, vendorId)) {
            throw new BusinessException("Vendor is not in your favorites");
        }
        favoriteRepository.deleteByUserIdAndVendorId(userId, vendorId);
        log.info("User {} removed vendor {} from favorites", userId, vendorId);
    }

    /**
     * Get all favorites for a customer.
     */
    @Transactional(readOnly = true)
    public List<FavoriteDTO> getFavorites(UUID userId) {
        return favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Check if a vendor is a favorite for the customer.
     */
    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID vendorId) {
        return favoriteRepository.existsByUserIdAndVendorId(userId, vendorId);
    }

    private FavoriteDTO toDTO(Favorite fav) {
        Vendor v = fav.getVendor();
        return FavoriteDTO.builder()
                .id(fav.getId())
                .vendorId(v.getId())
                .vendorName(v.getName())
                .vendorDescription(v.getDescription())
                .vendorAddress(v.getAddress())
                .rating(v.getRating() != null ? v.getRating().doubleValue() : null)
                .vendorActive(v.getActive())
                .createdAt(fav.getCreatedAt())
                .build();
    }
}
