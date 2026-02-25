/**
 * Device service — register/unregister push notification tokens
 * Maps to DeviceController: POST /api/devices/register, DELETE /api/devices/{token}
 */

import api from './api';

export interface RegisterDevicePayload {
  token: string;
  platform?: string;
}

export const deviceService = {
  /**
   * Register a device token for push notifications
   * POST /devices/register
   */
  registerDevice: async (token: string, platform?: string): Promise<any> => {
    const response = await api.post('/devices/register', { token, platform });
    return response;
  },

  /**
   * Unregister a device token
   * DELETE /devices/:token
   */
  unregisterDevice: async (token: string): Promise<void> => {
    await api.delete(`/devices/${encodeURIComponent(token)}`);
  },

  /**
   * Attempt to register the browser for push notifications.
   * Requests permission and registers the subscription token with the backend.
   */
  requestAndRegister: async (): Promise<boolean> => {
    try {
      if (!('Notification' in window) || !('serviceWorker' in navigator)) {
        console.log('Push notifications not supported');
        return false;
      }

      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        console.log('Push notification permission denied');
        return false;
      }

      // In a real app, we'd get the subscription from the service worker
      // and send the endpoint/keys to the backend. For now, use a placeholder.
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager?.getSubscription();

      if (subscription) {
        const token = JSON.stringify(subscription.toJSON());
        await deviceService.registerDevice(token, 'WEB');
        console.log('Push notification token registered');
        return true;
      }

      // Generate a simple browser identifier as fallback
      const browserId = `web-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
      await deviceService.registerDevice(browserId, 'WEB');
      console.log('Browser device registered (fallback)');
      return true;
    } catch (err) {
      console.warn('Failed to register for push notifications:', err);
      return false;
    }
  },
};

export default deviceService;
