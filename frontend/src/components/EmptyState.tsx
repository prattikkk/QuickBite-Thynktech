/**
 * EmptyState — contextual empty state with icon, title, description, and optional CTA
 */

import { ReactNode } from 'react';

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  className?: string;
}

const defaultIcons: Record<string, string> = {
  orders: '📦',
  favorites: '❤️',
  notifications: '🔔',
  cart: '🛒',
  search: '🔍',
  chat: '💬',
  reviews: '⭐',
  data: '📊',
  users: '👥',
};

export default function EmptyState({
  icon,
  title,
  description,
  action,
  className = '',
}: EmptyStateProps) {
  return (
    <div className={`flex flex-col items-center justify-center py-12 px-4 ${className}`}>
      <div className="text-5xl mb-4" aria-hidden="true">
        {icon ?? '📭'}
      </div>
      <h3 className="text-lg font-semibold text-gray-900 mb-2">{title}</h3>
      {description && (
        <p className="text-gray-500 text-center max-w-md mb-4">{description}</p>
      )}
      {action && (
        <button
          onClick={action.onClick}
          className="px-6 py-2 bg-orange-500 hover:bg-orange-600 text-white font-semibold rounded-lg transition-colors"
        >
          {action.label}
        </button>
      )}
    </div>
  );
}

export { defaultIcons };
