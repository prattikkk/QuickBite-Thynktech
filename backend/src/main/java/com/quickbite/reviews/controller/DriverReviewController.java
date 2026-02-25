package com.quickbite.reviews.controller;

import com.quickbite.common.dto.ApiResponse;
import com.quickbite.reviews.dto.DriverReviewDTO;
import com.quickbite.reviews.service.DriverReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DriverReviewController {

    private final DriverReviewService driverReviewService;

    /**
     * Submit a driver review for a delivered order.
     * POST /api/orders/{orderId}/driver-review
     */
    @PostMapping("/orders/{orderId}/driver-review")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<DriverReviewDTO>> submitDriverReview(
            @PathVariable UUID orderId,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        UUID customerId = UUID.fromString(authentication.getName());
        int rating = (Integer) body.get("rating");
        String comment = (String) body.get("comment");

        DriverReviewDTO review = driverReviewService.submitReview(orderId, customerId, rating, comment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Driver review submitted successfully", review));
    }

    /**
     * Get paginated reviews for a driver.
     * GET /api/drivers/{driverId}/reviews?page=0&size=10
     */
    @GetMapping("/drivers/{driverId}/reviews")
    public ResponseEntity<ApiResponse<Page<DriverReviewDTO>>> getDriverReviews(
            @PathVariable UUID driverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<DriverReviewDTO> reviews = driverReviewService.getDriverReviews(driverId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Driver reviews retrieved", reviews));
    }

    /**
     * Get rating summary for a driver.
     * GET /api/drivers/{driverId}/rating-summary
     */
    @GetMapping("/drivers/{driverId}/rating-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDriverRatingSummary(
            @PathVariable UUID driverId) {

        Map<String, Object> summary = driverReviewService.getDriverRatingSummary(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver rating summary retrieved", summary));
    }

    /**
     * Driver disputes a review.
     * PUT /api/driver-reviews/{reviewId}/dispute
     */
    @PutMapping("/driver-reviews/{reviewId}/dispute")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DriverReviewDTO>> disputeReview(
            @PathVariable UUID reviewId,
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        UUID driverId = UUID.fromString(authentication.getName());
        String reason = body.get("reason");

        DriverReviewDTO review = driverReviewService.disputeReview(reviewId, driverId, reason);
        return ResponseEntity.ok(ApiResponse.success("Review disputed successfully", review));
    }

    /**
     * Admin hides a driver review.
     * PUT /api/admin/driver-reviews/{reviewId}/hide
     */
    @PutMapping("/admin/driver-reviews/{reviewId}/hide")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hideReview(@PathVariable UUID reviewId) {
        driverReviewService.hideReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Driver review hidden", null));
    }
}
