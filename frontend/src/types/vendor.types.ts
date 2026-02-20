/**
 * Vendor and Menu DTOs - aligned with backend
 */

export interface VendorDTO {
  id: string;
  userId: string;
  name: string;
  description: string;
  address: string;
  lat: number;
  lng: number;
  openHours: Record<string, string>;
  rating: number;
  active: boolean;
  menuItemCount: number;
  createdAt: string;
  updatedAt: string;
}

export type VendorStatus = 'PENDING' | 'APPROVED' | 'SUSPENDED' | 'REJECTED';

export interface AddressDTO {
  id?: string;
  line1: string;
  line2?: string;
  city: string;
  state: string;
  postal: string;
  country: string;
  lat?: number;
  lng?: number;
}

export interface MenuItemDTO {
  id: string;
  vendorId: string;
  name: string;
  description: string;
  priceCents: number;
  price: number;
  category: string;
  imageUrl?: string;
  available: boolean;
  prepTimeMins: number;
  createdAt: string;
  updatedAt: string;
}

/** Request body to create a restaurant profile */
export interface VendorCreateRequest {
  name: string;
  description?: string;
  address?: string;
  lat?: number;
  lng?: number;
  openHours?: Record<string, string>;
}

/** Request body to update a restaurant profile */
export interface VendorUpdateRequest {
  name?: string;
  description?: string;
  address?: string;
  lat?: number;
  lng?: number;
  openHours?: Record<string, string>;
  active?: boolean;
}

/** Request body to create/update a menu item */
export interface MenuItemCreateRequest {
  name: string;
  description?: string;
  priceCents: number;
  available?: boolean;
  prepTimeMins?: number;
  category?: string;
  imageUrl?: string;
}

export interface VendorListResponse {
  content: VendorDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface MenuListResponse {
  content: MenuItemDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

