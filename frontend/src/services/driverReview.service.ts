/**
 * Driver review service — Phase 4.7
 */

import api from './api';
import type { DriverReviewDTO } from '../types/phase4.types';

export const driverReviewService = {
  /** Submit a review for the driver on a delivered order */
  async submitReview(orderId: string, rating: number, comment?: string): Promise<DriverReviewDTO> {
    return api.post(`/orders/${orderId}/driver-review`, { rating, comment }) as Promise<DriverReviewDTO>;
  },

  /** Get paginated reviews for a driver */
  async getDriverReviews(driverId: string, page = 0, size = 10): Promise<{
    content: DriverReviewDTO[];
    totalElements: number;
    totalPages: number;
  }> {
    return api.get(`/drivers/${driverId}/reviews`, { params: { page, size } }) as any;
  },

  /** Get rating summary for a driver */
  async getDriverRatingSummary(driverId: string): Promise<{ averageRating: number; totalReviews: number }> {
    return api.get(`/drivers/${driverId}/rating-summary`) as any;
  },

  /** Dispute a review (driver only) */
  async disputeReview(reviewId: string, reason: string): Promise<DriverReviewDTO> {
    return api.put(`/driver-reviews/${reviewId}/dispute`, { reason }) as Promise<DriverReviewDTO>;
  },
};
