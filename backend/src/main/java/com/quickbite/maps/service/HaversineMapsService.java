package com.quickbite.maps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Haversine-based maps service — dev fallback when no maps API key is configured.
 * Active when maps.provider=haversine (default).
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "maps.provider", havingValue = "haversine", matchIfMissing = true)
public class HaversineMapsService implements MapsService {

    @Override
    public LatLng geocode(String address) {
        log.info("[HaversineMaps] Geocode request (stub): {}", address);
        // Default coords for dev — central Delhi
        return new LatLng(BigDecimal.valueOf(28.6139), BigDecimal.valueOf(77.2090));
    }

    @Override
    public DistanceResult getDistance(double originLat, double originLng, double destLat, double destLng) {
        final double R = 6371.0;
        double dLat = Math.toRadians(destLat - originLat);
        double dLon = Math.toRadians(destLng - originLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(originLat)) * Math.cos(Math.toRadians(destLat))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double km = R * c;
        int mins = (int) Math.ceil((km / 25.0) * 60); // 25 km/h average
        return new DistanceResult(km, Math.max(5, mins));
    }
}
