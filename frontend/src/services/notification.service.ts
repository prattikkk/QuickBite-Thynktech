/**
 * Notification service — in-app notifications
 */

import api from './api';
import { NotificationDTO, NotificationPageResponse } from '../types';

export const notificationService = {
  /** Get paginated notifications */
  getNotifications: async (page = 0, size = 20): Promise<NotificationPageResponse> => {
    const res = await api.get<any, any>('/notifications', { params: { page, size } });
    return res?.data ?? res;
  },

  /** Get unread count */
  getUnreadCount: async (): Promise<number> => {
    const res = await api.get<any, any>('/notifications/unread-count');
    const data = res?.data ?? res;
    return data?.count ?? 0;
  },

  /** Mark single notification as read */
  markAsRead: async (id: string): Promise<NotificationDTO> => {
    const res = await api.patch<any, any>(`/notifications/${id}/read`);
    return res?.data ?? res;
  },

  /** Mark all as read */
  markAllAsRead: async (): Promise<void> => {
    await api.post('/notifications/read-all', {});
  },
};

export default notificationService;
