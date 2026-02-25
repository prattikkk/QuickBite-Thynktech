package com.quickbite.orders.mapper;

import com.quickbite.orders.dto.OrderCreateDTO;
import com.quickbite.orders.dto.OrderItemResponseDTO;
import com.quickbite.orders.dto.OrderResponseDTO;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderItem;
import com.quickbite.payments.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Manual mapper for Order entities and DTOs.
 * Could be replaced with MapStruct in production.
 */
@Component
public class OrderMapper {

    public OrderResponseDTO toResponseDTO(Order order) {
        if (order == null) {
            return null;
        }

        var address = order.getDeliveryAddress();
        var addressDTO = address != null ? OrderResponseDTO.AddressDTO.builder()
                .id(address.getId())
                .line1(address.getLine1())
                .line2(address.getLine2())
                .city(address.getCity())
                .state(address.getState())
                .postal(address.getPostal())
                .country(address.getCountry())
                .lat(address.getLat() != null ? address.getLat().doubleValue() : null)
                .lng(address.getLng() != null ? address.getLng().doubleValue() : null)
                .build() : null;

        // Extract Stripe client secret from payment (only for non-COD, pending payments)
        String paymentClientSecret = null;
        if (order.getPayment() != null && order.getPayment().getClientSecret() != null) {
            paymentClientSecret = order.getPayment().getClientSecret();
        }

        return OrderResponseDTO.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerPhone(order.getCustomer().getPhone())
                .vendorId(order.getVendor().getId())
                .vendorName(order.getVendor().getName())
                .vendorLat(order.getVendor().getLat() != null ? order.getVendor().getLat().doubleValue() : null)
                .vendorLng(order.getVendor().getLng() != null ? order.getVendor().getLng().doubleValue() : null)
                .driverId(order.getDriver() != null ? order.getDriver().getId() : null)
                .driverName(order.getDriver() != null ? order.getDriver().getName() : null)
                .driverPhone(order.getDriver() != null ? order.getDriver().getPhone() : null)
                .deliveryAddress(addressDTO)
                .items(order.getItems().stream()
                        .map(this::toOrderItemResponseDTO)
                        .collect(Collectors.toList()))
                .subtotalCents(order.getSubtotalCents())
                .deliveryFeeCents(order.getDeliveryFeeCents())
                .taxCents(order.getTaxCents())
                .discountCents(order.getDiscountCents())
                .totalCents(order.getTotalCents())
                .promoCode(order.getPromoCode())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .providerPaymentId(order.getPayment() != null ? order.getPayment().getProviderPaymentId() : null)
                .paymentClientSecret(paymentClientSecret)
                .scheduledTime(order.getScheduledTime())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .estimatedDeliveryAt(order.getEstimatedDeliveryAt())
                .estimatedPrepMins(order.getEstimatedPrepMins())
                .specialInstructions(order.getSpecialInstructions())
                .cancellationReason(order.getCancellationReason())
                .refundStatus(deriveRefundStatus(order))
                .commissionCents(order.getCommissionCents())
                .vendorPayoutCents(order.getVendorPayoutCents())
                .build();
    }

    private OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item) {
        return OrderItemResponseDTO.builder()
                .id(item.getId())
                .menuItemId(item.getMenuItem().getId())
                .name(item.getMenuItem().getName())
                .quantity(item.getQuantity())
                .unitPriceCents(item.getPriceCents())
                .totalCents(item.getPriceCents() * item.getQuantity())
                .specialInstructions(item.getSpecialInstructions())
                .build();
    }

    /**
     * Derive a human-readable refund status string from payment status.
     */
    private String deriveRefundStatus(Order order) {
        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            return "REFUNDED";
        }
        return null;
    }
}
