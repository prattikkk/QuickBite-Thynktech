/**
 * MenuItemCard component
 */

import React from 'react';
import { MenuItemDTO } from '../types';
import { formatCurrencyCompact } from '../utils';

interface MenuItemCardProps {
  menuItem: MenuItemDTO;
  onAddToCart: (item: MenuItemDTO) => void;
  disabled?: boolean;
}

export const MenuItemCard: React.FC<MenuItemCardProps> = ({
  menuItem,
  onAddToCart,
  disabled = false,
}) => {
  const handleAddClick = () => {
    if (!disabled && menuItem.available) {
      onAddToCart(menuItem);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow">
      {/* Image */}
      <div className="relative h-48 bg-gray-200">
        {menuItem.imageUrl ? (
          <img
            src={menuItem.imageUrl}
            alt={menuItem.name}
            className="w-full h-full object-cover"
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center text-gray-400">
            <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"
              />
            </svg>
          </div>
        )}

        {/* Badges */}
        <div className="absolute top-2 left-2 flex gap-2">
          {menuItem.category && (
            <span className="bg-blue-500 text-white text-xs px-2 py-1 rounded-full">
              {menuItem.category}
            </span>
          )}
          {!menuItem.available && (
            <span className="bg-red-500 text-white text-xs px-2 py-1 rounded-full">
              Unavailable
            </span>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="p-4">
        <div className="flex justify-between items-start mb-2">
          <h3 className="text-lg font-semibold text-gray-900 line-clamp-1">{menuItem.name}</h3>
          <span className="text-lg font-bold text-primary-600 ml-2 flex-shrink-0">
            {formatCurrencyCompact(menuItem.priceCents)}
          </span>
        </div>

        <p className="text-sm text-gray-600 mb-3 line-clamp-2">{menuItem.description}</p>

        <div className="flex items-center justify-between">
          <span className="text-xs text-gray-500">
            <svg
              className="inline w-4 h-4 mr-1"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
              />
            </svg>
            {menuItem.prepTimeMins || 15} mins
          </span>

          <button
            onClick={handleAddClick}
            disabled={disabled || !menuItem.available}
            className={`px-4 py-2 rounded-lg font-medium text-sm transition-colors ${
              disabled || !menuItem.available
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                : 'bg-primary-600 text-white hover:bg-primary-700'
            }`}
          >
            Add to Cart
          </button>
        </div>
      </div>
    </div>
  );
};

export default MenuItemCard;
