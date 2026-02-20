/**
 * Vendor service
 */

import api from './api';
import {
  VendorDTO,
  VendorListResponse,
  MenuItemDTO,
  PageRequest,
} from '../types';

export const vendorService = {
  /**
   * Get list of vendors with pagination
   */
  getVendors: async (params?: PageRequest): Promise<VendorListResponse> => {
    const response = await api.get<any, VendorListResponse>('/vendors', { params });
    return response;
  },

  /**
   * Get all vendors (fetches first 100 active vendors)
   */
  getAllVendors: async (): Promise<VendorDTO[]> => {
    const response = await api.get<any, VendorDTO[]>('/vendors', {
      params: { page: 0, size: 100 },
    });
    return Array.isArray(response) ? response : [];
  },

  /**
   * Get vendor by ID
   */
  getVendorById: async (id: string): Promise<VendorDTO> => {
    const response = await api.get<any, VendorDTO>(`/vendors/${id}`);
    return response;
  },

  /**
   * Get menu items for a vendor
   */
  getVendorMenu: async (vendorId: string, params?: PageRequest): Promise<MenuItemDTO[]> => {
    const response = await api.get<any, MenuItemDTO[]>(`/vendors/${vendorId}/menu`, {
      params,
    });
    return Array.isArray(response) ? response : [];
  },

  /**
   * Search vendors by name or cuisine
   */
  searchVendors: async (query: string, params?: PageRequest): Promise<VendorListResponse> => {
    const response = await api.get<any, VendorListResponse>('/vendors/search', {
      params: { ...params, query },
    });
    return response;
  },

  /**
   * Get orders for vendor (vendor dashboard)
   * Uses the generic /orders endpoint with vendorId filter
   */
  getVendorOrders: async (params?: PageRequest & { vendorId?: string }) => {
    const response = await api.get('/orders', { params });
    return response;
  },

  /**
   * Accept an order (vendor action)
   */
  acceptOrder: async (orderId: number | string): Promise<any> => {
    const response = await api.post(`/orders/${orderId}/accept`);
    return response;
  },

  /**
   * Reject an order (vendor action)
   */
  rejectOrder: async (orderId: number | string, reason?: string): Promise<any> => {
    const response = await api.post(`/orders/${orderId}/reject`, null, {
      params: { reason },
    });
    return response;
  },

  /**
   * Mark order as preparing
   */
  markOrderPreparing: async (orderId: number | string): Promise<any> => {
    const response = await api.patch(`/orders/${orderId}/status`, {
      status: 'PREPARING',
    });
    return response;
  },

  /**
   * Mark order as ready for pickup
   */
  markOrderReady: async (orderId: number | string): Promise<any> => {
    const response = await api.patch(`/orders/${orderId}/status`, {
      status: 'READY',
    });
    return response;
  },
};

export default vendorService;
