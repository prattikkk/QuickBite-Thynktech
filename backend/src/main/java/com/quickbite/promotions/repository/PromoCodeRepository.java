package com.quickbite.promotions.repository;

import com.quickbite.promotions.entity.PromoCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    List<PromoCode> findByActiveTrueOrderByCreatedAtDesc();

    boolean existsByCodeIgnoreCase(String code);
}
