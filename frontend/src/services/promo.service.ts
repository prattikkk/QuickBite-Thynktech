/**
 * Promo code service — validate and manage promo codes
 */

import api from './api';
import { PromoValidateResponse } from '../types';

export const promoService = {
  /** Validate a promo code for a given subtotal */
  validatePromo: async (code: string, subtotalCents: number): Promise<PromoValidateResponse> => {
    const res = await api.get<any, any>('/promos/validate', {
      params: { code, subtotalCents },
    });
    return res?.data ?? res;
  },

  /** List all promo codes (admin) */
  getPromos: async (): Promise<any[]> => {
    const res = await api.get<any, any>('/promos');
    const data = res?.data ?? res;
    return Array.isArray(data) ? data : data?.content || [];
  },

  /** Get a single promo code by ID */
  getPromo: async (id: string): Promise<any> => {
    const res = await api.get<any, any>(`/promos/${id}`);
    return res?.data ?? res;
  },

  /** Create a new promo code (admin) */
  createPromo: async (data: Record<string, any>): Promise<any> => {
    const res = await api.post<any, any>('/promos', data);
    return res?.data ?? res;
  },

  /** Update a promo code (admin) */
  updatePromo: async (id: string, data: Record<string, any>): Promise<any> => {
    const res = await api.put<any, any>(`/promos/${id}`, data);
    return res?.data ?? res;
  },

  /** Delete a promo code (admin) */
  deletePromo: async (id: string): Promise<void> => {
    await api.delete(`/promos/${id}`);
  },
};

export default promoService;
