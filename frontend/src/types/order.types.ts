/**
 * Order DTOs - aligned with backend
 */

import { AddressDTO } from './vendor.types';

export interface OrderDTO {
  id: string;
  orderNumber: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  vendorId: string;
  vendorName: string;
  driverId?: string;
  driverName?: string;
  driverPhone?: string;
  status: OrderStatus;
  items: OrderItemDTO[];
  deliveryAddress: AddressDTO;
  subtotalCents: number;
  deliveryFeeCents: number;
  taxCents: number;
  discountCents?: number;
  totalCents: number;
  promoCode?: string;
  paymentMethod: string;
  paymentStatus: string;
  /** Stripe PaymentIntent client secret — only present if card/online payment is pending */
  paymentClientSecret?: string;
  specialInstructions?: string;
  cancellationReason?: string;
  scheduledTime?: string;
  createdAt: string;
  updatedAt: string;
  deliveredAt?: string;
  estimatedDeliveryAt?: string;
  estimatedPrepMins?: number;
}

export type OrderStatus =
  | 'PLACED'
  | 'ACCEPTED'
  | 'PREPARING'
  | 'READY'
  | 'ASSIGNED'
  | 'PICKED_UP'
  | 'ENROUTE'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'REJECTED';

export type PaymentMethod = 'CASH_ON_DELIVERY' | 'CARD' | 'UPI';
export type PaymentStatus =
  | 'PENDING'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'FAILED'
  | 'REFUNDED'
  | 'CANCELLED';

export interface OrderItemDTO {
  id?: string;
  menuItemId: string;
  name: string;
  quantity: number;
  unitPriceCents: number;
  totalCents: number;
  specialInstructions?: string;
}

export interface OrderCreateRequest {
  items: OrderItemCreateRequest[];
  addressId: string;
  paymentMethod: PaymentMethod;
  specialInstructions?: string;
  promoCode?: string;
}

export interface OrderItemCreateRequest {
  menuItemId: string;
  quantity: number;
  specialInstructions?: string;
}

export interface OrderListResponse {
  content: OrderDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface OrderStatusUpdateRequest {
  status: OrderStatus;
  note?: string;
}

/**
 * Timeline entry from GET /api/admin/orders/:id/timeline
 */
export interface TimelineEntry {
  id: string;
  eventType: string;
  oldStatus?: string;
  newStatus?: string;
  actorId?: string;
  actorRole: string;
  meta?: Record<string, unknown>;
  createdAt: string;
}

/**
 * Structured error returned when an invalid order transition is attempted (400).
 */
export interface TransitionError {
  message: string;
  status: number;
  currentStatus?: string;
  targetStatus?: string;
  reason?: string;
}
