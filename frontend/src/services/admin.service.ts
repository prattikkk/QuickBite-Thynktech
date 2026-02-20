/**
 * Admin service — endpoints restricted to ADMIN role
 */

import api from './api';
import { TimelineEntry } from '../types';

export const adminService = {
  /**
   * Get order audit timeline
   * GET /admin/orders/:orderId/timeline
   */
  getOrderTimeline: async (orderId: string): Promise<TimelineEntry[]> => {
    const response = await api.get<any, TimelineEntry[]>(`/admin/orders/${orderId}/timeline`);
    return response;
  },
};

export default adminService;
