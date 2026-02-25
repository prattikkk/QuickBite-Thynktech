package com.quickbite.maps.service;

import java.math.BigDecimal;

/**
 * Abstraction for maps/geocoding operations.
 */
public interface MapsService {

    /**
     * Geocode an address string to lat/lng.
     *
     * @return LatLng or null if geocoding fails
     */
    LatLng geocode(String address);

    /**
     * Calculate driving distance and estimated travel time between two points.
     *
     * @return DistanceResult with distance in km and duration in minutes
     */
    DistanceResult getDistance(double originLat, double originLng, double destLat, double destLng);

    record LatLng(BigDecimal lat, BigDecimal lng) {}

    record DistanceResult(double distanceKm, int durationMinutes) {}
}
