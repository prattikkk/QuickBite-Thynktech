/**
 * Native Push Notifications plugin wrapper.
 *
 * Uses @capacitor/push-notifications on native platforms (FCM / APNs).
 * Falls back to the Web Push / Notification API on browsers / PWA.
 *
 * The wrapper handles registration, token management, and incoming
 * push events. The token should be sent to the backend so the server
 * can target individual devices.
 */

import { isNative } from './platform';

/* ── Types ───────────────────────────────────────────────────── */

export interface PushToken {
  /** The device push token (FCM registration token or APNs device token). */
  value: string;
}

export interface PushNotificationData {
  title: string;
  body: string;
  data?: Record<string, string>;
}

export type PushTokenListener = (token: PushToken) => void;
export type PushNotificationListener = (notification: PushNotificationData) => void;

/* ── State ───────────────────────────────────────────────────── */

const listeners: {
  tokenReceived: PushTokenListener[];
  notificationReceived: PushNotificationListener[];
  notificationAction: PushNotificationListener[];
} = {
  tokenReceived: [],
  notificationReceived: [],
  notificationAction: [],
};

let _initialized = false;
let _plugin: typeof import('@capacitor/push-notifications').PushNotifications | null = null;

async function getPlugin() {
  if (!_plugin) {
    const mod = await import('@capacitor/push-notifications');
    _plugin = mod.PushNotifications;
  }
  return _plugin;
}

/* ── Public API ──────────────────────────────────────────────── */

/**
 * Initialize push notification listeners.
 * Call once during app bootstrap (e.g., in App.tsx useEffect).
 */
export async function initPushNotifications(): Promise<void> {
  if (_initialized) return;
  _initialized = true;

  if (isNative()) {
    const push = await getPlugin();

    // Registration success — we have a device token
    await push.addListener('registration', (token) => {
      console.log('[Push] Registered, token:', token.value.substring(0, 20) + '…');
      listeners.tokenReceived.forEach((fn) => fn({ value: token.value }));
    });

    // Registration failed
    await push.addListener('registrationError', (err) => {
      console.error('[Push] Registration failed:', err);
    });

    // Notification received while app is in foreground
    await push.addListener('pushNotificationReceived', (notification) => {
      const data: PushNotificationData = {
        title: notification.title ?? '',
        body: notification.body ?? '',
        data: notification.data as Record<string, string>,
      };
      listeners.notificationReceived.forEach((fn) => fn(data));
    });

    // User tapped on a notification
    await push.addListener('pushNotificationActionPerformed', (action) => {
      const n = action.notification;
      const data: PushNotificationData = {
        title: n.title ?? '',
        body: n.body ?? '',
        data: n.data as Record<string, string>,
      };
      listeners.notificationAction.forEach((fn) => fn(data));
    });

    return;
  }

  // ── Web fallback — no auto-init needed (handled by usePWA / service worker) ──
  console.log('[Push] Web platform — push handled by service worker');
}

/**
 * Request push notification permission and register for tokens.
 */
export async function requestPushPermission(): Promise<'granted' | 'denied'> {
  if (isNative()) {
    const push = await getPlugin();
    const result = await push.requestPermissions();
    if (result.receive === 'granted') {
      await push.register();
      return 'granted';
    }
    return 'denied';
  }

  // Web fallback
  if (!('Notification' in window)) return 'denied';
  const permission = await Notification.requestPermission();
  return permission === 'granted' ? 'granted' : 'denied';
}

/**
 * Check current permission state without prompting.
 */
export async function checkPushPermission(): Promise<'granted' | 'denied' | 'prompt'> {
  if (isNative()) {
    const push = await getPlugin();
    const result = await push.checkPermissions();
    if (result.receive === 'granted') return 'granted';
    if (result.receive === 'denied') return 'denied';
    return 'prompt';
  }

  if (!('Notification' in window)) return 'denied';
  if (Notification.permission === 'granted') return 'granted';
  if (Notification.permission === 'denied') return 'denied';
  return 'prompt';
}

/* ── Listener registration ───────────────────────────────────── */

export function onTokenReceived(fn: PushTokenListener): () => void {
  listeners.tokenReceived.push(fn);
  return () => {
    listeners.tokenReceived = listeners.tokenReceived.filter((l) => l !== fn);
  };
}

export function onNotificationReceived(fn: PushNotificationListener): () => void {
  listeners.notificationReceived.push(fn);
  return () => {
    listeners.notificationReceived = listeners.notificationReceived.filter((l) => l !== fn);
  };
}

export function onNotificationAction(fn: PushNotificationListener): () => void {
  listeners.notificationAction.push(fn);
  return () => {
    listeners.notificationAction = listeners.notificationAction.filter((l) => l !== fn);
  };
}
