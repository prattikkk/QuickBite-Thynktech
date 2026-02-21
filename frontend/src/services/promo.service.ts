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
};

export default promoService;
