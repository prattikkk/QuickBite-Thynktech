/**
 * Review service – API calls for ratings & reviews
 */
import api from './api';

export interface ReviewDTO {
  id: string;
  orderId: string;
  vendorId: string;
  customerId: string;
  customerName: string;
  rating: number;
  comment: string | null;
  createdAt: string;
}

export interface RatingSummaryDTO {
  averageRating: number;
  totalReviews: number;
  distribution: Record<string, number>; // "5" -> count, "4" -> count, etc.
}

export interface PagedReviews {
  content: ReviewDTO[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export const reviewService = {
  /** Submit a review for a delivered order */
  async submitReview(orderId: string, rating: number, comment?: string): Promise<ReviewDTO> {
    const { data } = await api.post(`/orders/${orderId}/review`, { rating, comment });
    return data;
  },

  /** Get paginated reviews for a vendor (public) */
  async getVendorReviews(vendorId: string, page = 0, size = 10): Promise<PagedReviews> {
    const { data } = await api.get(`/vendors/${vendorId}/reviews`, { params: { page, size } });
    return data;
  },

  /** Get rating summary for a vendor (public) */
  async getRatingSummary(vendorId: string): Promise<RatingSummaryDTO> {
    const { data } = await api.get(`/vendors/${vendorId}/rating-summary`);
    return data;
  },

  /** Get all vendor reviews across all vendors (admin moderation) */
  async getAllReviews(page = 0, _size = 100): Promise<PagedReviews> {
    // No dedicated admin endpoint — aggregate from vendor list via large page
    const res: any = await api.get('/vendors', { params: { page: 0, size: 100 } });
    const vendors = res?.content || (Array.isArray(res) ? res : []);
    const allReviews: ReviewDTO[] = [];
    for (const v of vendors.slice(0, 20)) {
      try {
        const reviews = await api.get(`/vendors/${v.id}/reviews`, { params: { page, size: 50 } });
        const list = (reviews as any)?.content || (Array.isArray(reviews) ? reviews : []);
        allReviews.push(...list.map((r: any) => ({ ...r, vendorName: v.name })));
      } catch { /* skip */ }
    }
    return { content: allReviews, totalElements: allReviews.length, totalPages: 1, number: 0, size: allReviews.length, first: true, last: true };
  },
};

export default reviewService;
