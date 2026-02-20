/**
 * Order service — aligned with backend OrderController
 */

import api from './api';
import {
  OrderDTO,
  OrderCreateRequest,
  OrderListResponse,
  OrderStatusUpdateRequest,
  PageRequest,
  TimelineEntry,
} from '../types';

export const orderService = {
  /**
   * Create a new order
   * POST /orders
   */
  createOrder: async (orderData: OrderCreateRequest): Promise<OrderDTO> => {
    const response = await api.post<any, OrderDTO>('/orders', orderData);
    return response;
  },

  /**
   * Get order by ID
   * GET /orders/:orderId
   */
  getOrderById: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.get<any, OrderDTO>(`/orders/${orderId}`);
    return response;
  },

  /**
   * Get current user's orders (backend filters by authenticated user's role)
   * GET /orders
   */
  getMyOrders: async (params?: PageRequest): Promise<OrderListResponse> => {
    const response = await api.get<any, OrderListResponse>('/orders', { params });
    return response;
  },

  /**
   * Cancel an order
   * PATCH /orders/:orderId/status
   */
  cancelOrder: async (orderId: string, reason?: string): Promise<OrderDTO> => {
    const response = await api.patch<any, OrderDTO>(`/orders/${orderId}/status`, {
      status: 'CANCELLED',
      note: reason,
    });
    return response;
  },

  /**
   * Update order status (vendor/driver action)
   * PATCH /orders/:orderId/status
   */
  updateOrderStatus: async (
    orderId: string,
    statusUpdate: OrderStatusUpdateRequest
  ): Promise<OrderDTO> => {
    const response = await api.patch<any, OrderDTO>(`/orders/${orderId}/status`, statusUpdate);
    return response;
  },

  /**
   * Accept order (vendor)
   * POST /orders/:orderId/accept
   */
  acceptOrder: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.post<any, OrderDTO>(`/orders/${orderId}/accept`);
    return response;
  },

  /**
   * Reject order (vendor)
   * POST /orders/:orderId/reject
   */
  rejectOrder: async (orderId: string, reason?: string): Promise<OrderDTO> => {
    const response = await api.post<any, OrderDTO>(`/orders/${orderId}/reject`, null, {
      params: { reason },
    });
    return response;
  },

  /**
   * Get order status history
   * GET /orders/:orderId/status-history
   */
  getOrderStatusHistory: async (orderId: string): Promise<Array<{ status: string; timestamp: string; note: string }>> => {
    const response = await api.get<any, any>(`/orders/${orderId}/status-history`);
    return response;
  },

  /**
   * Get full audit timeline for an order (admin).
   * GET /admin/orders/:orderId/timeline
   */
  getOrderTimeline: async (orderId: string): Promise<TimelineEntry[]> => {
    const response = await api.get<any, TimelineEntry[]>(`/admin/orders/${orderId}/timeline`);
    return response;
  },
};

export default orderService;
