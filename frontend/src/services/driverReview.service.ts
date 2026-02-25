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

  /** Get all driver reviews across all drivers (admin moderation) */
  async getAllReviews(page = 0, _size = 100): Promise<{ content: DriverReviewDTO[]; totalElements: number }> {
    // Aggregate driver reviews from available drivers
    const res = await api.get('/admin/users', { params: { role: 'DRIVER', page: 0, size: 100 } }) as any;
    const drivers = res?.content || (Array.isArray(res) ? res : []);
    const allReviews: DriverReviewDTO[] = [];
    for (const d of drivers.slice(0, 20)) {
      try {
        const reviews = await api.get(`/drivers/${d.id}/reviews`, { params: { page, size: 50 } }) as any;
        const list = reviews?.content || (Array.isArray(reviews) ? reviews : []);
        allReviews.push(...list.map((r: any) => ({ ...r, driverName: d.fullName || d.name })));
      } catch { /* skip */ }
    }
    return { content: allReviews, totalElements: allReviews.length };
  },
};
