/**
 * Driver service — aligned with backend OrderController (DRIVER role)
 */

import api from './api';
import { OrderListResponse, OrderDTO, PageRequest } from '../types';

export const driverService = {
  /**
   * Get assigned orders for driver
   * Uses the generic /orders endpoint which filters by user role on backend
   */
  getAssignedOrders: async (params?: PageRequest): Promise<OrderListResponse> => {
    const response = await api.get<any, OrderListResponse>('/orders', { params });
    return response;
  },

  /**
   * Mark order as picked up (ASSIGNED/READY → PICKED_UP)
   */
  markPickedUp: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.patch<any, OrderDTO>(`/orders/${orderId}/status`, {
      status: 'PICKED_UP',
    });
    return response;
  },

  /**
   * Mark order as en route (PICKED_UP → ENROUTE)
   */
  markEnroute: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.patch<any, OrderDTO>(`/orders/${orderId}/status`, {
      status: 'ENROUTE',
    });
    return response;
  },

  /**
   * Mark order as delivered (ENROUTE → DELIVERED)
   */
  markDelivered: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.patch<any, OrderDTO>(`/orders/${orderId}/status`, {
      status: 'DELIVERED',
    });
    return response;
  },
};

export default driverService;
