/**
 * Platform detection utilities for Capacitor / PWA hybrid builds.
 *
 * Provides helpers to detect whether the app is running inside a
 * native Capacitor shell (Android / iOS) or in a standard browser / PWA.
 */

import { Capacitor } from '@capacitor/core';

/** True when running inside a Capacitor native shell. */
export const isNative = (): boolean => Capacitor.isNativePlatform();

/** True when running as an installed PWA (standalone display mode). */
export const isPWA = (): boolean =>
  typeof window !== 'undefined' &&
  (window.matchMedia('(display-mode: standalone)').matches ||
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (window.navigator as any).standalone === true);

/** Returns 'android' | 'ios' | 'web' */
export const getPlatform = (): 'android' | 'ios' | 'web' => {
  return Capacitor.getPlatform() as 'android' | 'ios' | 'web';
};

/** True when running in a plain browser tab (not PWA, not native). */
export const isBrowser = (): boolean => !isNative() && !isPWA();

/**
 * Resolve the API base URL depending on platform.
 *
 * - **Native (Capacitor):** must be an absolute URL because there is no
 *   reverse-proxy inside the native WebView.
 * - **Web / PWA:** use the relative `/api` path (forwarded by Vite proxy
 *   in dev or nginx in production).
 *
 * Set `VITE_NATIVE_API_URL` in `.env.capacitor` for native builds.
 * Falls back to `VITE_API_BASE_URL`, then `/api`.
 */
export const resolveApiBaseUrl = (): string => {
  if (isNative()) {
    // Native builds MUST have an absolute URL — relative paths fail
    return (
      import.meta.env.VITE_NATIVE_API_URL ||
      import.meta.env.VITE_API_BASE_URL ||
      'https://api.quickbite.com/api'
    );
  }
  // Web / PWA — relative path, proxied by server
  return import.meta.env.VITE_API_BASE_URL || '/api';
};

/**
 * Resolve the WebSocket base URL depending on platform.
 */
export const resolveWsUrl = (): string => {
  if (isNative()) {
    return (
      import.meta.env.VITE_NATIVE_WS_URL ||
      import.meta.env.VITE_WS_URL ||
      'wss://api.quickbite.com/ws'
    );
  }
  return import.meta.env.VITE_WS_URL || '/ws';
};
