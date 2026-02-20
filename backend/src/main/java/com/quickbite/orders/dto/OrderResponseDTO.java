package com.quickbite.orders.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for order response with full details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDTO {

    private UUID id;
    private String orderNumber;
    
    // Customer info
    private UUID customerId;
    private String customerName;
    private String customerPhone;
    
    // Vendor info
    private UUID vendorId;
    private String vendorName;
    
    // Driver info (optional)
    private UUID driverId;
    private String driverName;
    private String driverPhone;
    
    // Delivery address
    private AddressDTO deliveryAddress;
    
    // Order items
    private List<OrderItemResponseDTO> items;
    
    // Pricing
    private Long subtotalCents;
    private Long deliveryFeeCents;
    private Long taxCents;
    private Long totalCents;
    
    // Status
    private String status;
    private String paymentStatus;
    private String paymentMethod;

    /** Stripe PaymentIntent client secret — only present if online payment is pending. */
    private String paymentClientSecret;
    
    // Timestamps
    private OffsetDateTime scheduledTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deliveredAt;
    
    private String specialInstructions;
    private String cancellationReason;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDTO {
        private UUID id;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postal;
        private String country;
        private Double lat;
        private Double lng;
    }
}
