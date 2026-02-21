/**
 * Favorites page — list of customer's favorite vendors
 */

import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { favoriteService } from '../services';
import { FavoriteDTO } from '../types';
import { LoadingSpinner } from '../components';
import { useToastStore } from '../store';

export default function Favorites() {
  const [favorites, setFavorites] = useState<FavoriteDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const { error: showError, success } = useToastStore();

  useEffect(() => {
    loadFavorites();
  }, []);

  const loadFavorites = async () => {
    try {
      const data = await favoriteService.getFavorites();
      setFavorites(Array.isArray(data) ? data : []);
    } catch (err: any) {
      showError(err.message || 'Failed to load favorites');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (vendorId: string) => {
    try {
      await favoriteService.removeFavorite(vendorId);
      setFavorites((prev) => prev.filter((f) => f.vendorId !== vendorId));
      success('Removed from favorites');
    } catch (err: any) {
      showError(err.message || 'Failed to remove favorite');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">My Favorites</h1>

        {favorites.length === 0 ? (
          <div className="bg-white rounded-lg shadow-md p-12 text-center">
            <svg
              className="w-16 h-16 mx-auto text-gray-300 mb-4"
              fill="none"
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
            <p className="text-gray-600 mb-4">No favorites yet</p>
            <Link
              to="/vendors"
              className="inline-block px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
            >
              Browse Restaurants
            </Link>
          </div>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {favorites.map((fav) => (
              <div
                key={fav.id}
                className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
              >
                <div className="flex justify-between items-start">
                  <Link to={`/vendors/${fav.vendorId}`} className="flex-1">
                    <h3 className="text-lg font-bold text-gray-900 hover:text-primary-600">
                      {fav.vendorName}
                    </h3>
                    {fav.vendorDescription && (
                      <p className="text-sm text-gray-600 mt-1 line-clamp-2">
                        {fav.vendorDescription}
                      </p>
                    )}
                    {fav.vendorAddress && (
                      <p className="text-xs text-gray-500 mt-1">{fav.vendorAddress}</p>
                    )}
                    <div className="flex items-center gap-3 mt-2">
                      {fav.rating != null && (
                        <span className="inline-flex items-center text-sm text-yellow-600">
                          <svg className="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                            <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z" />
                          </svg>
                          {fav.rating.toFixed(1)}
                        </span>
                      )}
                      <span
                        className={`text-xs px-2 py-0.5 rounded-full ${
                          fav.vendorActive
                            ? 'bg-green-100 text-green-700'
                            : 'bg-gray-100 text-gray-600'
                        }`}
                      >
                        {fav.vendorActive ? 'Open' : 'Closed'}
                      </span>
                    </div>
                  </Link>

                  <button
                    onClick={() => handleRemove(fav.vendorId)}
                    className="p-2 text-red-500 hover:text-red-600 transition-colors"
                    title="Remove from favorites"
                  >
                    <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                      <path
                        d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"
                      />
                    </svg>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
