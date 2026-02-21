/**
 * Driver service — uses dedicated DriverController (/api/drivers/*)
 * for driver-specific features, plus generic OrderController for status changes.
 */

import api from './api';
import { OrderListResponse, OrderDTO, PageRequest } from '../types';

export interface DriverOrderSummary {
  orderId: string;
  status: string;
  totalCents: number;
  vendorId: string;
  vendorName: string;
  customerId: string;
  deliveryAddress?: {
    line1: string;
    city: string;
    state: string;
    postal: string;
  };
  createdAt: string;
}

export interface DriverProfileDTO {
  id: string;
  userId: string;
  name: string;
  email: string;
  vehicleType: string;
  licensePlate: string | null;
  isOnline: boolean;
  currentLat: number | null;
  currentLng: number | null;
  totalDeliveries: number;
  successRate: number;
}

export const driverService = {
  /**
   * Get assigned orders for driver (generic endpoint, filtered by role)
   */
  getAssignedOrders: async (params?: PageRequest): Promise<OrderListResponse> => {
    const response = await api.get<any, OrderListResponse>('/orders', { params });
    return response;
  },

  /**
   * Get available (unassigned READY) orders the driver can accept
   * Uses dedicated DriverController endpoint
   */
  getAvailableOrders: async (): Promise<DriverOrderSummary[]> => {
    const response = await api.get<any, DriverOrderSummary[]>('/drivers/available-orders');
    return response;
  },

  /**
   * Accept/claim an available order for delivery
   * Uses dedicated DriverController endpoint
   */
  acceptAvailableOrder: async (orderId: string): Promise<OrderDTO> => {
    const response = await api.post<any, OrderDTO>(`/drivers/orders/${orderId}/accept`);
    return response;
  },

  /**
   * Get the driver's current active delivery
   * Uses dedicated DriverController endpoint
   */
  getActiveDelivery: async (): Promise<DriverOrderSummary | null> => {
    const response = await api.get<any, DriverOrderSummary | null>('/drivers/active-delivery');
    return response;
  },

  /**
   * Update driver GPS location for current active delivery
   * Uses dedicated DriverController endpoint
   */
  updateLocation: async (lat: number, lng: number): Promise<void> => {
    await api.put('/drivers/location', { lat, lng });
  },

  /**
   * Get past delivery history (paginated)
   * Uses dedicated DriverController endpoint
   */
  getDeliveryHistory: async (page = 0, size = 20): Promise<DriverOrderSummary[]> => {
    const response = await api.get<any, DriverOrderSummary[]>('/drivers/delivery-history', {
      params: { page, size },
    });
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

  // ── Profile & Status ──────────────────────────────────

  /**
   * Get the driver's profile (vehicle info, stats, online status).
   */
  getProfile: async (): Promise<DriverProfileDTO> => {
    const response = await api.get<any, DriverProfileDTO>('/drivers/profile');
    return response;
  },

  /**
   * Update driver profile (vehicle type, license plate).
   */
  updateProfile: async (vehicleType: string, licensePlate: string): Promise<DriverProfileDTO> => {
    const response = await api.put<any, DriverProfileDTO>('/drivers/profile', {
      vehicleType,
      licensePlate,
    });
    return response;
  },

  /**
   * Toggle driver online/offline status.
   */
  toggleStatus: async (online: boolean): Promise<DriverProfileDTO> => {
    const response = await api.put<any, DriverProfileDTO>('/drivers/status', { online });
    return response;
  },
};

export default driverService;
