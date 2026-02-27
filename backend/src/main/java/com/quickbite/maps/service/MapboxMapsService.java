package com.quickbite.maps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Mapbox-based maps service — uses Mapbox Geocoding + Directions APIs.
 * Active when maps.provider=mapbox.
 *
 * Geocoding: https://api.mapbox.com/geocoding/v5/mapbox.places/{query}.json
 * Directions: https://api.mapbox.com/directions/v5/mapbox/driving/{coords}
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "maps.provider", havingValue = "mapbox")
public class MapboxMapsService implements MapsService {

    private final String accessToken;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String GEOCODE_URL = "https://api.mapbox.com/geocoding/v5/mapbox.places/%s.json?access_token=%s&limit=1&types=address,place,locality,neighborhood,poi";
    private static final String DIRECTIONS_URL = "https://api.mapbox.com/directions/v5/mapbox/driving/%s,%s;%s,%s?access_token=%s&overview=false&steps=false";

    public MapboxMapsService(@Value("${maps.mapbox.access-token}") String accessToken) {
        this.accessToken = accessToken;
        log.info("[MapboxMaps] Initialized with access token: {}...{}", accessToken.substring(0, 6), accessToken.substring(accessToken.length() - 4));
    }

    @Override
    @SuppressWarnings("unchecked")
    public LatLng geocode(String address) {
        try {
            String encoded = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = String.format(GEOCODE_URL, encoded, accessToken);
            log.debug("[MapboxMaps] Geocoding: {}", address);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> features = (List<Map<String, Object>>) response.get("features");
                if (features != null && !features.isEmpty()) {
                    List<Number> center = (List<Number>) features.get(0).get("center");
                    if (center != null && center.size() >= 2) {
                        double lng = center.get(0).doubleValue();
                        double lat = center.get(1).doubleValue();
                        log.debug("[MapboxMaps] Geocoded '{}' → ({}, {})", address, lat, lng);
                        return new LatLng(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
                    }
                }
            }
            log.warn("[MapboxMaps] Geocoding returned no results for: {}", address);
            return null;
        } catch (Exception e) {
            log.error("[MapboxMaps] Geocoding error for '{}': {}", address, e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public DistanceResult getDistance(double originLat, double originLng, double destLat, double destLng) {
        try {
            // Mapbox expects longitude,latitude order
            String url = String.format(DIRECTIONS_URL,
                    originLng, originLat,
                    destLng, destLat,
                    accessToken);
            log.debug("[MapboxMaps] Directions: ({},{}) → ({},{})", originLat, originLng, destLat, destLng);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    double distanceMeters = ((Number) route.get("distance")).doubleValue();
                    double durationSeconds = ((Number) route.get("duration")).doubleValue();

                    double km = distanceMeters / 1000.0;
                    int mins = (int) Math.ceil(durationSeconds / 60.0);

                    log.debug("[MapboxMaps] Route: {:.1f}km, {}min", km, mins);
                    return new DistanceResult(km, Math.max(1, mins));
                }
            }
        } catch (Exception e) {
            log.error("[MapboxMaps] Directions error: {}", e.getMessage());
        }

        // Fallback to Haversine
        return haversineFallback(originLat, originLng, destLat, destLng);
    }

    /**
     * Get driving route with full geometry (for frontend rendering).
     * Returns GeoJSON LineString coordinates.
     */
    @SuppressWarnings("unchecked")
    public RouteResult getRoute(double originLat, double originLng, double destLat, double destLng) {
        try {
            String url = String.format(
                    "https://api.mapbox.com/directions/v5/mapbox/driving/%s,%s;%s,%s?access_token=%s&overview=full&geometries=geojson&steps=true",
                    originLng, originLat, destLng, destLat, accessToken);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null) {
                List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
                if (routes != null && !routes.isEmpty()) {
                    Map<String, Object> route = routes.get(0);
                    double distanceMeters = ((Number) route.get("distance")).doubleValue();
                    double durationSeconds = ((Number) route.get("duration")).doubleValue();
                    Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");

                    // Extract step instructions
                    List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
                    List<Map<String, Object>> steps = legs != null && !legs.isEmpty()
                            ? (List<Map<String, Object>>) legs.get(0).get("steps")
                            : List.of();

                    return new RouteResult(
                            distanceMeters / 1000.0,
                            (int) Math.ceil(durationSeconds / 60.0),
                            geometry,
                            steps
                    );
                }
            }
        } catch (Exception e) {
            log.error("[MapboxMaps] Route error: {}", e.getMessage());
        }
        return null;
    }

    private DistanceResult haversineFallback(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double km = R * c;
        int mins = (int) Math.ceil((km / 25.0) * 60);
        return new DistanceResult(km, Math.max(5, mins));
    }

    /**
     * Route result with full geometry + steps for rendering.
     */
    public record RouteResult(
            double distanceKm,
            int durationMinutes,
            Map<String, Object> geometry,
            List<Map<String, Object>> steps
    ) {}
}
