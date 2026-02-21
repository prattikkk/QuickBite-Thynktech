package com.quickbite.favorites.repository;

import com.quickbite.favorites.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {

    List<Favorite> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Favorite> findByUserIdAndVendorId(UUID userId, UUID vendorId);

    boolean existsByUserIdAndVendorId(UUID userId, UUID vendorId);

    void deleteByUserIdAndVendorId(UUID userId, UUID vendorId);

    long countByUserId(UUID userId);
}
