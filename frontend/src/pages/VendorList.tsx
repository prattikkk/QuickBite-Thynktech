import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { vendorService } from '../services/vendor.service';
import { VendorDTO } from '../types/vendor.types';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { FavoriteButton } from '../components';
import { useToastStore } from '../store/toastStore';

export default function VendorList() {
  const [vendors, setVendors] = useState<VendorDTO[]>([]);
  const [filteredVendors, setFilteredVendors] = useState<VendorDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const { error: showError } = useToastStore();

  useEffect(() => {
    loadVendors();
  }, []);

  useEffect(() => {
    if (searchQuery.trim() === '') {
      setFilteredVendors(vendors);
    } else {
      const query = searchQuery.toLowerCase();
      const filtered = vendors.filter(
        (vendor) =>
          vendor.name.toLowerCase().includes(query) ||
          vendor.description?.toLowerCase().includes(query) ||
          vendor.address?.toLowerCase().includes(query)
      );
      setFilteredVendors(filtered);
    }
  }, [searchQuery, vendors]);

  const loadVendors = async () => {
    try {
      setLoading(true);
      const data = await vendorService.getAllVendors();
      setVendors(data);
      setFilteredVendors(data);
    } catch (error: any) {
      showError('Failed to load vendors. Please try again.');
      console.error('Error loading vendors:', error);
    } finally {
      setLoading(false);
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
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Restaurants near you</h1>
          <p className="text-gray-600">
            {vendors.length} {vendors.length === 1 ? 'restaurant' : 'restaurants'} available
          </p>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <input
              type="text"
              placeholder="Search by restaurant name, cuisine, or location..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full px-4 py-3 pl-12 border border-gray-300 rounded-lg shadow-sm focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            />
            <svg
              className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
              />
            </svg>
            {searchQuery && (
              <button
                onClick={() => setSearchQuery('')}
                className="absolute right-4 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
              >
                <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M6 18L18 6M6 6l12 12"
                  />
                </svg>
              </button>
            )}
          </div>
        </div>

        {/* Vendors Grid */}
        {filteredVendors.length === 0 ? (
          <div className="text-center py-12">
            <svg
              className="mx-auto h-12 w-12 text-gray-400"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"
              />
            </svg>
            <h3 className="mt-2 text-sm font-medium text-gray-900">No restaurants found</h3>
            <p className="mt-1 text-sm text-gray-500">
              {searchQuery ? 'Try adjusting your search' : 'No restaurants available at the moment'}
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredVendors.map((vendor) => (
              <Link
                key={vendor.id}
                to={`/vendors/${vendor.id}`}
                className="bg-white rounded-lg shadow-md hover:shadow-xl transition-shadow duration-300 overflow-hidden group"
              >
                {/* Vendor Image */}
                <div className="h-48 bg-gradient-to-br from-primary-400 to-primary-600 relative overflow-hidden">
                  <div className="w-full h-full flex items-center justify-center">
                    <svg
                      className="h-20 w-20 text-white opacity-50"
                      fill="none"
                      viewBox="0 0 24 24"
                      stroke="currentColor"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={1.5}
                        d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
                      />
                    </svg>
                  </div>
                  {/* Active Badge */}
                  <div className="absolute top-3 right-3 flex items-center gap-2">
                    {vendor.active && (
                      <span className="bg-green-500 text-white text-xs font-semibold px-2 py-1 rounded-full">
                        Open
                      </span>
                    )}
                    <FavoriteButton vendorId={vendor.id} className="bg-white/80 backdrop-blur-sm shadow-sm" />
                  </div>
                </div>

                {/* Vendor Info */}
                <div className="p-5">
                  <h3 className="text-lg font-semibold text-gray-900 mb-1 line-clamp-1">
                    {vendor.name}
                  </h3>

                  {vendor.description && (
                    <p className="text-sm text-gray-600 mb-2 line-clamp-2">{vendor.description}</p>
                  )}

                  {vendor.rating && vendor.rating > 0 && (
                    <div className="flex items-center mb-3">
                      <svg
                        className="h-4 w-4 text-yellow-400 fill-current"
                        viewBox="0 0 20 20"
                      >
                        <path d="M10 15l-5.878 3.09 1.123-6.545L.489 6.91l6.572-.955L10 0l2.939 5.955 6.572.955-4.756 4.635 1.123 6.545z" />
                      </svg>
                      <span className="ml-1 text-sm font-medium text-gray-700">
                        {vendor.rating.toFixed(1)}
                      </span>
                    </div>
                  )}

                  {vendor.address && (
                    <div className="flex items-start text-sm text-gray-600 mb-3">
                      <svg
                        className="h-4 w-4 mt-0.5 mr-1.5 flex-shrink-0"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
                        />
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
                        />
                      </svg>
                      <span className="line-clamp-2">{vendor.address}</span>
                    </div>
                  )}

                  {vendor.menuItemCount > 0 && (
                    <div className="flex items-center text-sm text-gray-600">
                      <svg
                        className="h-4 w-4 mr-1.5"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 6v6m0 0v6m0-6h6m-6 0H6"
                        />
                      </svg>
                      <span>{vendor.menuItemCount} items</span>
                    </div>
                  )}

                  {/* View Menu Button */}
                  <div className="mt-4 pt-4 border-t border-gray-100">
                    <span className="text-primary-600 font-medium text-sm group-hover:text-primary-700">
                      View Menu →
                    </span>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

