/**
 * DriverRatings — driver ratings tab showing reviews and summary.
 * Phase 4.7
 */

import { useState, useEffect } from 'react';
import { driverReviewService } from '../services/driverReview.service';
import type { DriverReviewDTO } from '../types/phase4.types';
import StarRating from './StarRating';

interface DriverRatingsProps {
  driverId: string;
}

export default function DriverRatings({ driverId }: DriverRatingsProps) {
  const [reviews, setReviews] = useState<DriverReviewDTO[]>([]);
  const [summary, setSummary] = useState<{ averageRating: number; totalReviews: number } | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      driverReviewService.getDriverReviews(driverId, page, 10),
      driverReviewService.getDriverRatingSummary(driverId),
    ])
      .then(([reviewData, summaryData]) => {
        setReviews(reviewData.content);
        setTotalPages(reviewData.totalPages);
        setSummary(summaryData);
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [driverId, page]);

  if (loading) {
    return <div className="flex justify-center py-8"><div className="animate-spin h-8 w-8 border-b-2 border-primary-600 rounded-full" /></div>;
  }

  return (
    <div className="space-y-6">
      {/* Rating Summary */}
      {summary && (
        <div className="bg-white rounded-lg shadow p-6 flex items-center gap-6">
          <div className="text-center">
            <p className="text-4xl font-bold text-gray-900">{summary.averageRating.toFixed(1)}</p>
            <StarRating rating={summary.averageRating} size="md" />
            <p className="text-sm text-gray-500 mt-1">{summary.totalReviews} reviews</p>
          </div>
        </div>
      )}

      {/* Reviews List */}
      {reviews.length === 0 ? (
        <p className="text-gray-500 text-center py-8">No reviews yet.</p>
      ) : (
        <div className="space-y-4">
          {reviews.map((review) => (
            <div key={review.id} className="bg-white rounded-lg shadow p-4">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <StarRating rating={review.rating} size="sm" />
                  <span className="text-sm font-medium text-gray-900">{review.customerName}</span>
                </div>
                <span className="text-xs text-gray-400">
                  {new Date(review.createdAt).toLocaleDateString()}
                </span>
              </div>
              {review.comment && (
                <p className="text-sm text-gray-700">{review.comment}</p>
              )}
              <p className="text-xs text-gray-400 mt-1">Order #{review.orderNumber}</p>
              {review.disputed && (
                <span className="inline-block mt-2 px-2 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-700">
                  Disputed
                </span>
              )}
            </div>
          ))}

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex justify-center gap-2 pt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1 text-sm rounded border disabled:opacity-50"
              >
                Previous
              </button>
              <span className="px-3 py-1 text-sm text-gray-500">
                {page + 1} / {totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1 text-sm rounded border disabled:opacity-50"
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
