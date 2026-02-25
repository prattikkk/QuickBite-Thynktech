package com.quickbite.reviews.service;

import com.quickbite.notifications.entity.NotificationType;
import com.quickbite.notifications.service.NotificationService;
import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.reviews.dto.DriverReviewDTO;
import com.quickbite.reviews.entity.DriverReview;
import com.quickbite.reviews.repository.DriverReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverReviewService {

    private final DriverReviewRepository driverReviewRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * Submit a driver review for a delivered order.
     */
    @Transactional
    public DriverReviewDTO submitReview(UUID orderId, UUID customerId, int rating, String comment) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Order does not belong to this customer");
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Can only review delivered orders. Current status: " + order.getStatus());
        }

        if (order.getDriver() == null) {
            throw new BusinessException("Order has no assigned driver");
        }

        if (driverReviewRepository.existsByOrderIdAndCustomerId(orderId, customerId)) {
            throw new BusinessException("You have already reviewed the driver for this order");
        }

        if (rating < 1 || rating > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }

        DriverReview review = DriverReview.builder()
                .order(order)
                .customer(order.getCustomer())
                .driver(order.getDriver())
                .rating(rating)
                .comment(comment)
                .build();

        review = driverReviewRepository.save(review);

        // Update driver average rating check
        checkDriverAverageRating(order.getDriver().getId());

        log.info("Driver review submitted: orderId={} driverId={} rating={}",
                orderId, order.getDriver().getId(), rating);

        return toDTO(review);
    }

    /**
     * Get paginated reviews for a driver.
     */
    @Transactional(readOnly = true)
    public Page<DriverReviewDTO> getDriverReviews(UUID driverId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return driverReviewRepository.findByDriverIdAndHiddenFalseOrderByCreatedAtDesc(driverId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get rating summary for a driver.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDriverRatingSummary(UUID driverId) {
        Double avg = driverReviewRepository.findAverageRatingByDriverId(driverId);
        long total = driverReviewRepository.countByDriverIdAndHiddenFalse(driverId);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("averageRating", avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        summary.put("totalReviews", total);
        return summary;
    }

    /**
     * Driver disputes a review.
     */
    @Transactional
    public DriverReviewDTO disputeReview(UUID reviewId, UUID driverId, String reason) {
        DriverReview review = driverReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("Driver review not found: " + reviewId));

        if (!review.getDriver().getId().equals(driverId)) {
            throw new BusinessException("Review does not belong to this driver");
        }

        if (review.getDisputed()) {
            throw new BusinessException("Review is already disputed");
        }

        review.setDisputed(true);
        review.setDisputeReason(reason);
        review = driverReviewRepository.save(review);

        log.info("Driver review disputed: reviewId={} driverId={}", reviewId, driverId);

        return toDTO(review);
    }

    /**
     * Admin hides a review.
     */
    @Transactional
    public void hideReview(UUID reviewId) {
        DriverReview review = driverReviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("Driver review not found: " + reviewId));
        review.setHidden(true);
        driverReviewRepository.save(review);
        log.info("Driver review hidden: {}", reviewId);
    }

    /**
     * Check driver average rating and notify admin if below threshold.
     */
    private void checkDriverAverageRating(UUID driverId) {
        Double avg = driverReviewRepository.findAverageRatingByDriverId(driverId);
        if (avg != null && avg < 3.5) {
            log.warn("Driver {} has average rating {} (below 3.5 threshold)", driverId, String.format("%.1f", avg));
            try {
                notificationService.createNotification(
                        driverId,
                        NotificationType.DRIVER_RATING,
                        "Low Driver Rating Alert",
                        String.format("Driver %s has an average rating of %.1f which is below the 3.5 threshold.", driverId, avg),
                        driverId
                );
            } catch (Exception e) {
                log.warn("Failed to send low-rating notification for driver {}: {}", driverId, e.getMessage());
            }
        }
    }

    private DriverReviewDTO toDTO(DriverReview review) {
        return DriverReviewDTO.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .orderNumber(review.getOrder().getOrderNumber())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getName())
                .driverId(review.getDriver().getId())
                .driverName(review.getDriver().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .disputed(review.getDisputed())
                .disputeReason(review.getDisputeReason())
                .hidden(review.getHidden())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
