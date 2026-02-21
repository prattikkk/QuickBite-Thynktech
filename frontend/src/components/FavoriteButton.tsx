/**
 * FavoriteButton — heart icon to toggle vendor favorite status
 */

import { useState, useEffect } from 'react';
import { favoriteService } from '../services';
import { useToastStore } from '../store';
import { useAuth } from '../hooks';

interface Props {
  vendorId: string;
  className?: string;
}

export default function FavoriteButton({ vendorId, className = '' }: Props) {
  const [isFavorite, setIsFavorite] = useState(false);
  const [loading, setLoading] = useState(false);
  const { success, error: showError } = useToastStore();
  const { isAuthenticated, isCustomer } = useAuth();

  useEffect(() => {
    if (isAuthenticated && isCustomer()) {
      checkFavorite();
    }
  }, [vendorId, isAuthenticated]);

  const checkFavorite = async () => {
    try {
      const result = await favoriteService.isFavorite(vendorId);
      setIsFavorite(result);
    } catch {
      // ignore — not critical
    }
  };

  const toggle = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (loading) return;

    try {
      setLoading(true);
      if (isFavorite) {
        await favoriteService.removeFavorite(vendorId);
        setIsFavorite(false);
        success('Removed from favorites');
      } else {
        await favoriteService.addFavorite(vendorId);
        setIsFavorite(true);
        success('Added to favorites');
      }
    } catch (err: any) {
      showError(err.message || 'Failed to update favorite');
    } finally {
      setLoading(false);
    }
  };

  if (!isAuthenticated || !isCustomer()) return null;

  return (
    <button
      onClick={toggle}
      disabled={loading}
      className={`p-2 rounded-full transition-colors ${
        isFavorite
          ? 'text-red-500 hover:text-red-600'
          : 'text-gray-400 hover:text-red-400'
      } ${className}`}
      title={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
      aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
    >
      <svg
        className="w-6 h-6"
        fill={isFavorite ? 'currentColor' : 'none'}
        stroke="currentColor"
        viewBox="0 0 24 24"
      >
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          strokeWidth={2}
          d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
        />
      </svg>
    </button>
  );
}
