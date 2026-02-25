/**
 * ReviewForm – submit a rating + comment for a delivered order
 */
import { useState } from 'react';
import StarRating from './StarRating';
import { reviewService, ReviewDTO } from '../services/review.service';
import { useToastStore } from '../store';

interface ReviewFormProps {
  orderId: string;
  onReviewSubmitted?: (review: ReviewDTO) => void;
}

export default function ReviewForm({ orderId, onReviewSubmitted }: ReviewFormProps) {
  const [rating, setRating] = useState(0);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);
  const { success, error: showError } = useToastStore();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0) {
      showError('Please select a star rating');
      return;
    }

    try {
      setSubmitting(true);
      const review = await reviewService.submitReview(orderId, rating, comment || undefined);
      setSubmitted(true);
      success('Thank you for your review!');
      onReviewSubmitted?.(review);
    } catch (err: any) {
      showError(err.message || 'Failed to submit review');
    } finally {
      setSubmitting(false);
    }
  };

  if (submitted) {
    return (
      <div className="bg-green-50 border border-green-200 rounded-lg p-4 text-center">
        <svg className="w-8 h-8 mx-auto text-green-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
        </svg>
        <p className="text-green-800 font-medium">Review submitted!</p>
        <div className="mt-2">
          <StarRating rating={rating} size="sm" />
        </div>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white border border-gray-200 rounded-lg p-4">
      <h3 className="text-lg font-semibold text-gray-900 mb-3">Rate your order</h3>

      <div className="mb-4">
        <label className="block text-sm text-gray-600 mb-1">Your rating</label>
        <StarRating rating={rating} size="lg" interactive onChange={setRating} />
        {rating > 0 && (
          <p className="text-sm text-gray-500 mt-1">
            {rating === 5 && 'Excellent!'}
            {rating === 4 && 'Very Good'}
            {rating === 3 && 'Good'}
            {rating === 2 && 'Fair'}
            {rating === 1 && 'Poor'}
          </p>
        )}
      </div>

      <div className="mb-4">
        <label className="block text-sm text-gray-600 mb-1">Comment (optional)</label>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          placeholder="Tell others about your experience..."
          rows={3}
          maxLength={500}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-primary-500 focus:border-primary-500"
        />
        <p className="text-xs text-gray-400 text-right">{comment.length}/500</p>
      </div>

      <button
        type="submit"
        disabled={submitting || rating === 0}
        className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-medium"
      >
        {submitting ? 'Submitting...' : 'Submit Review'}
      </button>
    </form>
  );
}
