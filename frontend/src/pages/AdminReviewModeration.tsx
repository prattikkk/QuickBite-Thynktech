/**
 * AdminReviewModeration — hide/unhide vendor and driver reviews (M1)
 * Backend: PUT /admin/reviews/{id}/hide, PUT /admin/driver-reviews/{id}/hide
 */

import { useState, useEffect } from 'react';
import { adminService } from '../services/admin.service';
import { reviewService } from '../services/review.service';
import { driverReviewService } from '../services/driverReview.service';
import { useToastStore } from '../store';
import Breadcrumbs from '../components/Breadcrumbs';
import EmptyState from '../components/EmptyState';
import ConfirmDialog from '../components/ConfirmDialog';
import { SkeletonTable } from '../components/Skeleton';
import StarRating from '../components/StarRating';

type ReviewTab = 'vendor' | 'driver';

interface ReviewItem {
  id: string;
  rating: number;
  comment: string;
  customerName?: string;
  vendorName?: string;
  driverName?: string;
  hidden: boolean;
  createdAt: string;
}

export default function AdminReviewModeration() {
  const [tab, setTab] = useState<ReviewTab>('vendor');
  const [reviews, setReviews] = useState<ReviewItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [ratingFilter, setRatingFilter] = useState<number | null>(null);
  const [hideTarget, setHideTarget] = useState<ReviewItem | null>(null);
  const [actionLoading, setActionLoading] = useState(false);
  const { success, error: showError } = useToastStore();

  useEffect(() => {
    loadReviews();
  }, [tab]);

  const loadReviews = async () => {
    try {
      setLoading(true);
      if (tab === 'vendor') {
        // Get all vendor reviews via admin endpoint or public vendor reviews
        const data = await reviewService.getAllReviews();
        const list = Array.isArray(data) ? data : data?.content || [];
        setReviews(
          list.map((r: any) => ({
            id: r.id,
            rating: r.rating,
            comment: r.comment || '',
            customerName: r.customerName || r.userName || 'Anonymous',
            vendorName: r.vendorName || '',
            hidden: r.hidden ?? false,
            createdAt: r.createdAt,
          }))
        );
      } else {
        const data = await driverReviewService.getAllReviews();
        const list = Array.isArray(data) ? data : data?.content || [];
        setReviews(
          list.map((r: any) => ({
            id: r.id,
            rating: r.rating,
            comment: r.comment || '',
            customerName: r.customerName || r.userName || 'Anonymous',
            driverName: r.driverName || '',
            hidden: r.hidden ?? false,
            createdAt: r.createdAt,
          }))
        );
      }
    } catch (err: any) {
      showError(err.message || 'Failed to load reviews');
      setReviews([]);
    } finally {
      setLoading(false);
    }
  };

  const handleToggleHide = async () => {
    if (!hideTarget) return;
    try {
      setActionLoading(true);
      if (tab === 'vendor') {
        await adminService.hideVendorReview(hideTarget.id, !hideTarget.hidden);
      } else {
        await adminService.hideDriverReview(hideTarget.id, !hideTarget.hidden);
      }
      success(hideTarget.hidden ? 'Review unhidden' : 'Review hidden');
      setHideTarget(null);
      loadReviews();
    } catch (err: any) {
      showError(err.message || 'Failed to update review');
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = reviews.filter((r) => {
    if (ratingFilter !== null && r.rating !== ratingFilter) return false;
    return true;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 py-8">
      <Breadcrumbs items={[{ label: 'Admin', to: '/admin/health' }, { label: 'Reviews' }]} />

      <h1 className="text-2xl font-bold text-gray-900 dark:text-white mb-6">Review Moderation</h1>

      {/* Tabs */}
      <div className="flex gap-4 mb-6 border-b border-gray-200 dark:border-gray-700">
        {(['vendor', 'driver'] as const).map((t) => (
          <button
            key={t}
            onClick={() => { setTab(t); setRatingFilter(null); }}
            className={`pb-3 px-1 text-sm font-medium border-b-2 transition-colors ${
              tab === t
                ? 'border-orange-500 text-orange-600'
                : 'border-transparent text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200'
            }`}
          >
            {t === 'vendor' ? '🏪 Vendor Reviews' : '🚗 Driver Reviews'}
          </button>
        ))}
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 mb-4">
        <span className="text-sm text-gray-600 dark:text-gray-300">Filter by rating:</span>
        {[null, 5, 4, 3, 2, 1].map((r) => (
          <button
            key={r ?? 'all'}
            onClick={() => setRatingFilter(r)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-colors ${
              ratingFilter === r
                ? 'bg-orange-500 text-white'
                : 'bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-200 hover:bg-gray-200 dark:hover:bg-gray-600'
            }`}
          >
            {r === null ? 'All' : `${r}★`}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow-sm p-6">
          <SkeletonTable rows={5} cols={5} />
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          icon="📝"
          title="No reviews found"
          description={ratingFilter ? 'No reviews match the selected rating filter.' : `No ${tab} reviews available.`}
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((review) => (
            <div
              key={review.id}
              className={`bg-white dark:bg-gray-800 rounded-lg shadow-sm border p-4 ${review.hidden ? 'opacity-60 border-red-200' : 'border-gray-200 dark:border-gray-700'}`}
            >
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3 mb-1">
                    <StarRating rating={review.rating} size="sm" />
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {review.customerName}
                    </span>
                    {review.hidden && (
                      <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                        Hidden
                      </span>
                    )}
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400 mb-2">
                    {tab === 'vendor' ? `Vendor: ${review.vendorName}` : `Driver: ${review.driverName}`}
                    {' · '}
                    {new Date(review.createdAt).toLocaleDateString()}
                  </p>
                  {review.comment && (
                    <p className="text-sm text-gray-700 dark:text-gray-200">{review.comment}</p>
                  )}
                </div>
                <button
                  onClick={() => setHideTarget(review)}
                  className={`text-sm font-medium px-3 py-1 rounded-lg transition-colors ${
                    review.hidden
                      ? 'text-green-600 hover:bg-green-50'
                      : 'text-red-600 hover:bg-red-50'
                  }`}
                >
                  {review.hidden ? 'Unhide' : 'Hide'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={!!hideTarget}
        title={hideTarget?.hidden ? 'Unhide Review' : 'Hide Review'}
        message={
          hideTarget?.hidden
            ? 'This review will become visible to all users again.'
            : 'This review will be hidden from public view. The reviewer will not be notified.'
        }
        confirmLabel={hideTarget?.hidden ? 'Unhide' : 'Hide'}
        variant={hideTarget?.hidden ? 'default' : 'danger'}
        loading={actionLoading}
        onConfirm={handleToggleHide}
        onCancel={() => setHideTarget(null)}
      />
    </div>
  );
}
