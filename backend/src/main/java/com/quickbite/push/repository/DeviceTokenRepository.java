package com.quickbite.push.repository;

import com.quickbite.push.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    List<DeviceToken> findByUserId(UUID userId);

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    void deleteByUserIdAndToken(UUID userId, String token);

    void deleteByToken(String token);

    long countByUserId(UUID userId);
}
