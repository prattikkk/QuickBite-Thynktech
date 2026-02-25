package com.quickbite.maps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

/**
 * Google Maps implementation — uses Geocoding + Distance Matrix APIs.
 * Active when maps.provider=google.
 *
 * Falls back to Haversine when API key is not configured.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "maps.provider", havingValue = "google")
public class GoogleMapsService implements MapsService {

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleMapsService(@Value("${maps.google.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public LatLng geocode(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/geocode/json?address="
                    + java.net.URLEncoder.encode(address, "UTF-8") + "&key=" + apiKey;
            var response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && "OK".equals(response.get("status"))) {
                var results = (java.util.List<?>) response.get("results");
                if (!results.isEmpty()) {
                    var location = ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) ((java.util.Map<?, ?>) results.get(0))
                            .get("geometry")).get("location"));
                    double lat = ((Number) location.get("lat")).doubleValue();
                    double lng = ((Number) location.get("lng")).doubleValue();
                    return new LatLng(BigDecimal.valueOf(lat), BigDecimal.valueOf(lng));
                }
            }
            log.warn("Geocoding failed for address: {}", address);
            return null;
        } catch (Exception e) {
            log.error("Geocoding error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public DistanceResult getDistance(double originLat, double originLng, double destLat, double destLng) {
        try {
            String origin = originLat + "," + originLng;
            String dest = destLat + "," + destLng;
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" + origin
                    + "&destinations=" + dest + "&mode=driving&key=" + apiKey;
            var response = restTemplate.getForObject(url, java.util.Map.class);
            if (response != null && "OK".equals(response.get("status"))) {
                var rows = (java.util.List<?>) response.get("rows");
                if (!rows.isEmpty()) {
                    var elements = (java.util.List<?>) ((java.util.Map<?, ?>) rows.get(0)).get("elements");
                    var element = (java.util.Map<?, ?>) elements.get(0);
                    if ("OK".equals(element.get("status"))) {
                        var distance = (java.util.Map<?, ?>) element.get("distance");
                        var duration = (java.util.Map<?, ?>) element.get("duration");
                        double km = ((Number) distance.get("value")).doubleValue() / 1000.0;
                        int mins = (int) Math.ceil(((Number) duration.get("value")).doubleValue() / 60.0);
                        return new DistanceResult(km, mins);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Distance matrix error: {}", e.getMessage());
        }
        // Fallback to Haversine
        return haversineFallback(originLat, originLng, destLat, destLng);
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
}
