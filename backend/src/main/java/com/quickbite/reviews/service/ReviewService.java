package com.quickbite.reviews.service;

import com.quickbite.orders.entity.Order;
import com.quickbite.orders.entity.OrderStatus;
import com.quickbite.orders.exception.BusinessException;
import com.quickbite.orders.repository.OrderRepository;
import com.quickbite.reviews.dto.RatingSummaryDTO;
import com.quickbite.reviews.dto.ReviewCreateRequest;
import com.quickbite.reviews.dto.ReviewDTO;
import com.quickbite.reviews.entity.Review;
import com.quickbite.reviews.repository.ReviewRepository;
import com.quickbite.vendors.entity.Vendor;
import com.quickbite.vendors.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;

    /**
     * Submit a review for a delivered order.
     */
    @Transactional
    public ReviewDTO submitReview(UUID orderId, UUID customerId, ReviewCreateRequest request) {
        // Validate order exists and belongs to this customer
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order not found: " + orderId));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new BusinessException("Order does not belong to this customer");
        }

        // Must be DELIVERED
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Can only review delivered orders. Current status: " + order.getStatus());
        }

        // Check for duplicate review
        if (reviewRepository.existsByOrderIdAndCustomerId(orderId, customerId)) {
            throw new BusinessException("You have already reviewed this order");
        }

        Review review = Review.builder()
                .order(order)
                .customer(order.getCustomer())
                .vendor(order.getVendor())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        review = reviewRepository.save(review);

        // Update vendor's average rating
        updateVendorRating(order.getVendor().getId());

        log.info("Review submitted: orderId={} vendorId={} rating={}",
                orderId, order.getVendor().getId(), request.getRating());

        return toDTO(review);
    }

    /**
     * Get paginated reviews for a vendor.
     */
    @Transactional(readOnly = true)
    public Page<ReviewDTO> getVendorReviews(UUID vendorId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return reviewRepository.findByVendorIdAndHiddenFalseOrderByCreatedAtDesc(vendorId, pageable)
                .map(this::toDTO);
    }

    /**
     * Get rating summary for a vendor.
     */
    @Transactional(readOnly = true)
    public RatingSummaryDTO getRatingSummary(UUID vendorId) {
        Double avg = reviewRepository.findAverageRatingByVendorId(vendorId);
        long total = reviewRepository.countByVendorIdAndHiddenFalse(vendorId);
        List<Object[]> distRows = reviewRepository.findRatingDistributionByVendorId(vendorId);

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) distribution.put(i, 0L);
        for (Object[] row : distRows) {
            Integer star = (Integer) row[0];
            Long count = (Long) row[1];
            distribution.put(star, count);
        }

        return RatingSummaryDTO.builder()
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .totalReviews(total)
                .distribution(distribution)
                .build();
    }

    /**
     * Admin: hide a review.
     */
    @Transactional
    public void hideReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException("Review not found"));
        review.setHidden(true);
        reviewRepository.save(review);
        updateVendorRating(review.getVendor().getId());
        log.info("Review hidden: {}", reviewId);
    }

    /**
     * Recalculate and update vendor's average rating.
     */
    private void updateVendorRating(UUID vendorId) {
        Double avg = reviewRepository.findAverageRatingByVendorId(vendorId);
        Vendor vendor = vendorRepository.findById(vendorId).orElse(null);
        if (vendor != null && avg != null) {
            vendor.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            vendorRepository.save(vendor);
        }
    }

    private ReviewDTO toDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .orderId(review.getOrder().getId())
                .vendorId(review.getVendor().getId())
                .customerId(review.getCustomer().getId())
                .customerName(review.getCustomer().getName())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
