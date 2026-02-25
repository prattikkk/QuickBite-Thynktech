package com.quickbite.reviews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RatingSummaryDTO {
    private Double averageRating;
    private Long totalReviews;
    /** e.g. {5: 10, 4: 8, 3: 3, 2: 1, 1: 0} */
    private Map<Integer, Long> distribution;
}
