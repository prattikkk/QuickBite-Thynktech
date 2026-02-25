/**
 * Analytics service — Phase 4.2 / 4.3
 */

import api from './api';
import type { VendorAnalytics, PlatformKpis } from '../types/phase4.types';

export const analyticsService = {
  /** Get vendor performance analytics */
  async getVendorAnalytics(vendorId: string, period = 'weekly'): Promise<VendorAnalytics> {
    return api.get(`/vendors/${vendorId}/analytics`, { params: { period } }) as Promise<VendorAnalytics>;
  },

  /** Export vendor analytics as CSV */
  async exportVendorCsv(vendorId: string, period = 'monthly'): Promise<Blob> {
    return api.get(`/vendors/${vendorId}/analytics/export`, {
      params: { period },
      responseType: 'blob',
    }) as Promise<Blob>;
  },

  /** Get platform KPIs (admin) */
  async getPlatformKpis(period = 'monthly'): Promise<PlatformKpis> {
    return api.get('/admin/reports/kpis', { params: { period } }) as Promise<PlatformKpis>;
  },

  /** Get revenue report (admin) */
  async getRevenueReport(period = 'weekly'): Promise<Record<string, unknown>[]> {
    return api.get('/admin/reports/revenue', { params: { period } }) as Promise<Record<string, unknown>[]>;
  },

  /** Get delivery time stats (admin) */
  async getDeliveryTimeReport(): Promise<Record<string, unknown>> {
    return api.get('/admin/reports/delivery-times') as Promise<Record<string, unknown>>;
  },

  /** Export admin report as CSV */
  async exportAdminCsv(type: string, period = 'monthly'): Promise<Blob> {
    return api.get('/admin/reports/export', {
      params: { type, period },
      responseType: 'blob',
    }) as Promise<Blob>;
  },
};
