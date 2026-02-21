/**
 * Admin service — endpoints restricted to ADMIN role
 */

import api from './api';
import { TimelineEntry } from '../types';

export interface HealthSummary {
  dbPool: {
    activeConnections: number;
    idleConnections: number;
    totalConnections: number;
    threadsAwaitingConnection: number;
  };
  webhooks: {
    pendingCount: number;
    dlqCount: number;
  };
  metrics: {
    ordersCreated: number;
    orderTransitions: number;
    paymentIntentsCreated: number;
    paymentSuccess: number;
    paymentFailed: number;
    webhooksProcessed: number;
    webhooksFailed: number;
    webhooksDlq: number;
  };
  featureFlags: Record<string, boolean>;
  totalTimelineEvents: number;
}

export const adminService = {
  /**
   * Get order audit timeline
   * GET /admin/orders/:orderId/timeline
   */
  getOrderTimeline: async (orderId: string): Promise<TimelineEntry[]> => {
    const response = await api.get<any, TimelineEntry[]>(`/admin/orders/${orderId}/timeline`);
    return response;
  },

  /**
   * Get operational health summary
   * GET /admin/health-summary
   */
  getHealthSummary: async (): Promise<HealthSummary> => {
    const response = await api.get<any, HealthSummary>('/admin/health-summary');
    return response;
  },

  /**
   * Get all feature flags
   * GET /admin/feature-flags
   */
  getFeatureFlags: async (): Promise<Record<string, boolean>> => {
    const response = await api.get<any, Record<string, boolean>>('/admin/feature-flags');
    return response;
  },

  /**
   * Toggle a feature flag
   * PUT /admin/feature-flags/:key
   */
  toggleFeatureFlag: async (key: string, enabled: boolean): Promise<any> => {
    const response = await api.put(`/admin/feature-flags/${key}`, { enabled });
    return response;
  },
};

export default adminService;
