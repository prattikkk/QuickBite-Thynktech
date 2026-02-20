/**
 * CartWidget component - displays cart icon with item count
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { useCartStore } from '../store';

export const CartWidget: React.FC = () => {
  const { getTotalItems } = useCartStore();
  const itemCount = getTotalItems();

  return (
    <Link
      to="/cart"
      className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors"
      aria-label="Shopping cart"
    >
      <svg
        className="w-6 h-6 text-gray-700"
        fill="none"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="2"
        viewBox="0 0 24 24"
        stroke="currentColor"
      >
        <path d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z" />
      </svg>

      {itemCount > 0 && (
        <span className="absolute -top-1 -right-1 bg-primary-600 text-white text-xs font-bold rounded-full h-5 w-5 flex items-center justify-center">
          {itemCount > 9 ? '9+' : itemCount}
        </span>
      )}
    </Link>
  );
};

export default CartWidget;
