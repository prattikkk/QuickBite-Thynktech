/**
 * Notification types
 */

export type NotificationType = 'ORDER_UPDATE' | 'PROMO' | 'SYSTEM' | 'DRIVER_ASSIGNED' | 'PAYMENT';

export interface NotificationDTO {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  refId?: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationPageResponse {
  content: NotificationDTO[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
