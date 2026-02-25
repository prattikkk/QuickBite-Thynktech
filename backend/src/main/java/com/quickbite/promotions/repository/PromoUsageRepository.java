package com.quickbite.promotions.repository;

import com.quickbite.promotions.entity.PromoUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PromoUsageRepository extends JpaRepository<PromoUsage, UUID> {

    long countByUserIdAndPromoId(UUID userId, UUID promoId);

    boolean existsByUserIdAndPromoId(UUID userId, UUID promoId);

    long countByUserId(UUID userId);
}
