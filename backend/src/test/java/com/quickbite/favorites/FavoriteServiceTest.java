package com.quickbite.favorites;

import com.quickbite.favorites.dto.FavoriteDTO;
import com.quickbite.favorites.entity.Favorite;
import com.quickbite.favorites.repository.FavoriteRepository;
import com.quickbite.favorites.service.FavoriteService;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.users.entity.User;
import com.quickbite.users.repository.UserRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FavoriteService — Phase 3 favorites feature.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private FavoriteRepository favoriteRepository;
    @Mock private UserRepository userRepository;
    @Mock private VendorRepository vendorRepository;

    private FavoriteService favoriteService;

    private UUID customerId;
    private UUID vendorId;
    private User customer;
    private Vendor vendor;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteRepository, userRepository, vendorRepository);

        customerId = UUID.randomUUID();
        vendorId = UUID.randomUUID();

        customer = User.builder()
                .id(customerId)
                .email("customer@test.com")
                .name("Test Customer")
                .build();

        vendor = Vendor.builder()
                .id(vendorId)
                .user(User.builder().id(UUID.randomUUID()).build())
                .name("Test Restaurant")
                .description("Great food")
                .address("123 Main St")
                .active(true)
                .build();
    }

    @Test
    @DisplayName("addFavorite — success adds vendor to favorites")
    void addFavorite_success() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(false);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor));
        when(favoriteRepository.save(any(Favorite.class))).thenAnswer(inv -> {
            Favorite f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            f.setCreatedAt(OffsetDateTime.now());
            return f;
        });

        FavoriteDTO dto = favoriteService.addFavorite(customerId, vendorId);

        assertThat(dto).isNotNull();
        assertThat(dto.getVendorId()).isEqualTo(vendorId);
        assertThat(dto.getVendorName()).isEqualTo("Test Restaurant");
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("addFavorite — duplicate throws exception")
    void addFavorite_duplicate_throws() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(true);

        assertThatThrownBy(() -> favoriteService.addFavorite(customerId, vendorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already");

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    @DisplayName("removeFavorite — success removes vendor from favorites")
    void removeFavorite_success() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(true);

        favoriteService.removeFavorite(customerId, vendorId);

        verify(favoriteRepository).deleteByUserIdAndVendorId(customerId, vendorId);
    }

    @Test
    @DisplayName("removeFavorite — non-existent throws exception")
    void removeFavorite_notFound_throws() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(false);

        assertThatThrownBy(() -> favoriteService.removeFavorite(customerId, vendorId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not in your favorites");
    }

    @Test
    @DisplayName("getFavorites — returns list of favorites")
    void getFavorites_success() {
        Favorite fav = Favorite.builder()
                .id(UUID.randomUUID())
                .user(customer)
                .vendor(vendor)
                .createdAt(OffsetDateTime.now())
                .build();

        when(favoriteRepository.findByUserIdOrderByCreatedAtDesc(customerId))
                .thenReturn(List.of(fav));

        List<FavoriteDTO> result = favoriteService.getFavorites(customerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getVendorName()).isEqualTo("Test Restaurant");
    }

    @Test
    @DisplayName("isFavorite — returns true when favorited")
    void isFavorite_true() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(true);

        boolean result = favoriteService.isFavorite(customerId, vendorId);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("isFavorite — returns false when not favorited")
    void isFavorite_false() {
        when(favoriteRepository.existsByUserIdAndVendorId(customerId, vendorId)).thenReturn(false);

        boolean result = favoriteService.isFavorite(customerId, vendorId);

        assertThat(result).isFalse();
    }
}
