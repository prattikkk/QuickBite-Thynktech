/**
 * Common types and interfaces
 */

export interface ApiError {
  message: string;
  status: number;
  errors?: Record<string, string[]>;
  timestamp?: string;
  path?: string;
}

export interface PageRequest {
  page?: number;
  size?: number;
  sort?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

// Cart item (frontend-only type)
export interface CartItem {
  menuItemId: string;
  menuItemName: string;
  vendorId: string;
  vendorName: string;
  priceCents: number;
  quantity: number;
  imageUrl?: string;
}
