package com.quickbite.orders.service;

import com.quickbite.maps.service.MapsService;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.vendors.entity.Vendor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * ETA calculation service.
 * Estimates delivery time based on vendor prep time and distance.
 * Optionally uses MapsService for real distance calculation.
 */
@Slf4j
@Service
public class EtaService {

    private final MapsService mapsService;

    /** Average speed in km/h for delivery driver */
    private static final double AVERAGE_SPEED_KMH = 25.0;

    /** Fixed overhead in minutes (order processing, pickup, etc.) */
    private static final int OVERHEAD_MINS = 5;

    public EtaService(MapsService mapsService) {
        this.mapsService = mapsService;
    }

    /**
     * Estimate delivery time for an order.
     *
     * @param order the order to estimate
     * @return estimated delivery OffsetDateTime
     */
    public OffsetDateTime estimateDelivery(Order order) {
        int prepMins = estimatePrepTime(order);
        int travelMins = estimateTravelTime(order);
        int totalMins = prepMins + travelMins + OVERHEAD_MINS;

        OffsetDateTime eta = OffsetDateTime.now().plusMinutes(totalMins);
        log.debug("ETA for order {}: prep={}min, travel={}min, overhead={}min → {}",
                order.getId(), prepMins, travelMins, OVERHEAD_MINS, eta);
        return eta;
    }

    /**
     * Estimate preparation time based on order items.
     * Uses the maximum prep time across all items (parallel preparation).
     */
    public int estimatePrepTime(Order order) {
        return order.getItems().stream()
                .filter(item -> item.getMenuItem() != null)
                .mapToInt(item -> {
                    Integer prep = item.getMenuItem().getPrepTimeMins();
                    return prep != null ? prep : 15; // default 15 min
                })
                .max()
                .orElse(15);
    }

    /**
     * Estimate travel time using MapsService (falls back to Haversine).
     */
    public int estimateTravelTime(Order order) {
        Vendor vendor = order.getVendor();
        var address = order.getDeliveryAddress();

        if (vendor.getLat() == null || vendor.getLng() == null
                || address.getLat() == null || address.getLng() == null) {
            // Fallback when coordinates are missing
            return 20;
        }

        try {
            MapsService.DistanceResult dist = mapsService.getDistance(
                    vendor.getLat().doubleValue(), vendor.getLng().doubleValue(),
                    address.getLat().doubleValue(), address.getLng().doubleValue()
            );
            int travelMins = (int) Math.ceil(dist.durationMinutes());
            return Math.max(travelMins, 5);
        } catch (Exception e) {
            log.warn("MapsService distance failed, falling back to Haversine: {}", e.getMessage());
        }

        double distKm = haversineDistance(
                vendor.getLat().doubleValue(), vendor.getLng().doubleValue(),
                address.getLat().doubleValue(), address.getLng().doubleValue()
        );

        int travelMins = (int) Math.ceil((distKm / AVERAGE_SPEED_KMH) * 60);
        return Math.max(travelMins, 5); // minimum 5 minutes
    }

    /**
     * Haversine formula for great-circle distance between two points.
     *
     * @return distance in kilometres
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
