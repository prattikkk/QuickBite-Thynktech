package com.quickbite.reviews.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.reviews.dto.RatingSummaryDTO;
import com.quickbite.reviews.dto.ReviewCreateRequest;
import com.quickbite.reviews.dto.ReviewDTO;
import com.quickbite.reviews.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Submit a review for a delivered order.
     * POST /api/orders/{orderId}/review
     */
    @PostMapping("/orders/{orderId}/review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ReviewDTO>> submitReview(
            @PathVariable UUID orderId,
            @Valid @RequestBody ReviewCreateRequest request,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());
        ReviewDTO review = reviewService.submitReview(orderId, customerId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }

    /**
     * Get paginated reviews for a vendor (public).
     * GET /api/vendors/{vendorId}/reviews
     */
    @GetMapping("/vendors/{vendorId}/reviews")
    public ResponseEntity<ApiResponse<Page<ReviewDTO>>> getVendorReviews(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewDTO> reviews = reviewService.getVendorReviews(vendorId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Vendor reviews retrieved", reviews));
    }

    /**
     * Get rating summary for a vendor (public).
     * GET /api/vendors/{vendorId}/rating-summary
     */
    @GetMapping("/vendors/{vendorId}/rating-summary")
    public ResponseEntity<ApiResponse<RatingSummaryDTO>> getRatingSummary(
            @PathVariable UUID vendorId) {

        RatingSummaryDTO summary = reviewService.getRatingSummary(vendorId);
        return ResponseEntity.ok(ApiResponse.success("Rating summary retrieved", summary));
    }

    /**
     * Admin: hide a review.
     * PUT /api/admin/reviews/{reviewId}/hide
     */
    @PutMapping("/admin/reviews/{reviewId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hideReview(@PathVariable UUID reviewId) {
        reviewService.hideReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review hidden", null));
    }
}
