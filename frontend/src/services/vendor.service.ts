/**
 * Vendor service — browsing, profile management, menu CRUD, order management
 */

import api from './api';
import {
  VendorDTO,
  VendorListResponse,
  VendorCreateRequest,
  VendorUpdateRequest,
  MenuItemDTO,
  MenuItemCreateRequest,
  PageRequest,
} from '../types';

export const vendorService = {
  // ── Browse (all users) ─────────────────────────────────────────────

  getVendors: async (params?: PageRequest): Promise<VendorListResponse> => {
    return api.get<any, VendorListResponse>('/vendors', { params });
  },

  getAllVendors: async (): Promise<VendorDTO[]> => {
    const response = await api.get<any, VendorDTO[]>('/vendors', {
      params: { page: 0, size: 100 },
    });
    return Array.isArray(response) ? response : [];
  },

  getVendorById: async (id: string): Promise<VendorDTO> => {
    return api.get<any, VendorDTO>(`/vendors/${id}`);
  },

  getVendorMenu: async (vendorId: string, params?: PageRequest): Promise<MenuItemDTO[]> => {
    const response = await api.get<any, MenuItemDTO[]>(`/vendors/${vendorId}/menu`, { params });
    return Array.isArray(response) ? response : [];
  },

  searchVendors: async (query: string, params?: PageRequest): Promise<VendorListResponse> => {
    return api.get<any, VendorListResponse>('/vendors/search', {
      params: { ...params, query },
    });
  },

  // ── Vendor profile management ──────────────────────────────────────

  /** Get the logged-in vendor's own restaurant profile (null if none yet) */
  getMyVendorProfile: async (): Promise<VendorDTO | null> => {
    const response = await api.get<any, VendorDTO | null>('/vendors/my');
    return response ?? null;
  },

  /** Create a new restaurant for the logged-in vendor */
  createVendorProfile: async (data: VendorCreateRequest): Promise<VendorDTO> => {
    return api.post<any, VendorDTO>('/vendors', data);
  },

  /** Update the logged-in vendor's own restaurant */
  updateVendorProfile: async (data: VendorUpdateRequest): Promise<VendorDTO> => {
    return api.put<any, VendorDTO>('/vendors/my', data);
  },

  // ── Menu item CRUD (for vendor owners) ─────────────────────────────

  /** Add a new menu item to the vendor's restaurant */
  createMenuItem: async (vendorId: string, data: MenuItemCreateRequest): Promise<MenuItemDTO> => {
    return api.post<any, MenuItemDTO>(`/vendors/${vendorId}/menu`, data);
  },

  /** Update an existing menu item */
  updateMenuItem: async (itemId: string, data: MenuItemCreateRequest): Promise<MenuItemDTO> => {
    return api.put<any, MenuItemDTO>(`/menu-items/${itemId}`, data);
  },

  /** Delete a menu item */
  deleteMenuItem: async (itemId: string): Promise<void> => {
    await api.delete(`/menu-items/${itemId}`);
  },

  // ── Order management (vendor dashboard) ────────────────────────────

  getVendorOrders: async (params?: PageRequest & { vendorId?: string }) => {
    return api.get('/orders', { params });
  },

  acceptOrder: async (orderId: string): Promise<any> => {
    return api.post(`/orders/${orderId}/accept`);
  },

  rejectOrder: async (orderId: string, reason?: string): Promise<any> => {
    return api.post(`/orders/${orderId}/reject`, null, { params: { reason } });
  },

  markOrderPreparing: async (orderId: string): Promise<any> => {
    return api.patch(`/orders/${orderId}/status`, { status: 'PREPARING' });
  },

  markOrderReady: async (orderId: string): Promise<any> => {
    return api.patch(`/orders/${orderId}/status`, { status: 'READY' });
  },
};

export default vendorService;
