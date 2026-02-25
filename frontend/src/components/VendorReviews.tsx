/**
 * VendorReviews – display reviews + rating summary for a vendor
 */
import { useState, useEffect } from 'react';
import StarRating from './StarRating';
import { reviewService, ReviewDTO, RatingSummaryDTO, PagedReviews } from '../services/review.service';

interface VendorReviewsProps {
  vendorId: string;
}

export default function VendorReviews({ vendorId }: VendorReviewsProps) {
  const [summary, setSummary] = useState<RatingSummaryDTO | null>(null);
  const [reviews, setReviews] = useState<ReviewDTO[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, [vendorId]);

  useEffect(() => {
    loadReviews();
  }, [page, vendorId]);

  const loadData = async () => {
    try {
      const s = await reviewService.getRatingSummary(vendorId);
      setSummary(s);
    } catch {
      // silent
    }
  };

  const loadReviews = async () => {
    try {
      setLoading(true);
      const data: PagedReviews = await reviewService.getVendorReviews(vendorId, page, 5);
      setReviews(data.content);
      setTotalPages(data.totalPages);
    } catch {
      // silent
    } finally {
      setLoading(false);
    }
  };

  const distributionBars = summary
    ? [5, 4, 3, 2, 1].map((star) => ({
        star,
        count: summary.distribution[String(star)] ?? 0,
        pct: summary.totalReviews ? ((summary.distribution[String(star)] ?? 0) / summary.totalReviews) * 100 : 0,
      }))
    : [];

  return (
    <div>
      {/* Rating Summary */}
      {summary && summary.totalReviews > 0 && (
        <div className="bg-white border border-gray-200 rounded-lg p-6 mb-6">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Customer Reviews</h3>
          <div className="flex flex-col sm:flex-row gap-6">
            {/* Big number */}
            <div className="text-center sm:text-left">
              <p className="text-5xl font-bold text-gray-900">{summary.averageRating.toFixed(1)}</p>
              <StarRating rating={summary.averageRating} size="md" className="justify-center sm:justify-start mt-1" />
              <p className="text-sm text-gray-500 mt-1">{summary.totalReviews} review{summary.totalReviews !== 1 ? 's' : ''}</p>
            </div>
            {/* Distribution bars */}
            <div className="flex-1 space-y-1">
              {distributionBars.map((bar) => (
                <div key={bar.star} className="flex items-center gap-2 text-sm">
                  <span className="w-3 text-right text-gray-600">{bar.star}</span>
                  <svg className="w-4 h-4 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                  </svg>
                  <div className="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                    <div className="h-full bg-yellow-400 rounded-full" style={{ width: `${bar.pct}%` }} />
                  </div>
                  <span className="w-8 text-gray-500 text-right">{bar.count}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Reviews List */}
      {loading && reviews.length === 0 ? (
        <p className="text-gray-500 text-center py-4">Loading reviews...</p>
      ) : reviews.length === 0 ? (
        <p className="text-gray-500 text-center py-4">No reviews yet</p>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div key={review.id} className="bg-white border border-gray-200 rounded-lg p-4">
              <div className="flex items-start justify-between mb-2">
                <div>
                  <p className="font-medium text-gray-900">{review.customerName}</p>
                  <StarRating rating={review.rating} size="sm" />
                </div>
                <p className="text-xs text-gray-400">{new Date(review.createdAt).toLocaleDateString()}</p>
              </div>
              {review.comment && <p className="text-gray-700 text-sm">{review.comment}</p>}
            </div>
          ))}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 pt-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 text-sm border rounded-md disabled:opacity-50"
              >
                Previous
              </button>
              <span className="px-3 py-1 text-sm text-gray-600">
                Page {page + 1} of {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-sm border rounded-md disabled:opacity-50"
              >
                Next
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
