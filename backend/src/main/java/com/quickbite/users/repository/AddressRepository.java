package com.quickbite.users.repository;

import com.quickbite.users.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Address entity operations.
 * Manages user delivery addresses.
 */
@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * Find all addresses for a user.
     *
     * @param userId the user ID
     * @return List of addresses
     */
    List<Address> findByUserId(UUID userId);

    /**
     * Find the default address for a user.
     *
     * @param userId the user ID
     * @return Optional containing the default address if found
     */
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") UUID userId);

    /**
     * Find addresses by city.
     *
     * @param city the city name
     * @return List of addresses in the specified city
     */
    List<Address> findByCity(String city);

    /**
     * Count addresses for a user.
     *
     * @param userId the user ID
     * @return count of addresses
     */
    Long countByUserId(UUID userId);

    /**
     * Delete all addresses for a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
