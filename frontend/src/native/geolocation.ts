/**
 * Native Geolocation plugin wrapper.
 *
 * Uses @capacitor/geolocation on native platforms and falls back
 * to the standard Web Geolocation API on browsers / PWA.
 *
 * This abstraction allows the existing `useDriverLocation` hook to
 * work unchanged while gaining native GPS fidelity on mobile.
 */

import { isNative } from './platform';

/* ── Types (shared with Web API) ─────────────────────────────── */

export interface GeoPosition {
  lat: number;
  lng: number;
  accuracy: number;
  speed: number | null;
  heading: number | null;
  timestamp: number;
}

export interface GeoWatchOptions {
  enableHighAccuracy?: boolean;
  timeout?: number;
  maximumAge?: number;
}

export type GeoSuccessCallback = (pos: GeoPosition) => void;
export type GeoErrorCallback = (err: { code: number; message: string }) => void;

/* ── Public API ──────────────────────────────────────────────── */

let _capacitorGeolocation: typeof import('@capacitor/geolocation').Geolocation | null = null;
let _callbackId: string | null = null;

/**
 * Lazy-load the Capacitor Geolocation plugin only on native.
 * This avoids bundling it in pure web builds.
 */
async function getPlugin() {
  if (!_capacitorGeolocation) {
    const mod = await import('@capacitor/geolocation');
    _capacitorGeolocation = mod.Geolocation;
  }
  return _capacitorGeolocation;
}

/**
 * Start watching the device position.
 *
 * Returns a disposer function that stops the watch.
 */
export async function watchPosition(
  onSuccess: GeoSuccessCallback,
  onError: GeoErrorCallback,
  options: GeoWatchOptions = {},
): Promise<() => void> {
  if (isNative()) {
    const geo = await getPlugin();

    // Request permission first (native requires explicit ask)
    const perm = await geo.requestPermissions();
    if (perm.location !== 'granted' && perm.coarseLocation !== 'granted') {
      onError({ code: 1, message: 'Location permission denied' });
      return () => {};
    }

    _callbackId = await geo.watchPosition(
      {
        enableHighAccuracy: options.enableHighAccuracy ?? true,
        timeout: options.timeout ?? 15000,
        maximumAge: options.maximumAge ?? 3000,
      },
      (position, err) => {
        if (err) {
          onError({ code: -1, message: err.message ?? 'Unknown geolocation error' });
          return;
        }
        if (position) {
          onSuccess({
            lat: position.coords.latitude,
            lng: position.coords.longitude,
            accuracy: position.coords.accuracy,
            speed: position.coords.speed,
            heading: position.coords.heading,
            timestamp: position.timestamp,
          });
        }
      },
    );

    return () => {
      if (_callbackId) {
        geo.clearWatch({ id: _callbackId });
        _callbackId = null;
      }
    };
  }

  // ── Web fallback ────────────────────────────────────────────
  const watchId = navigator.geolocation.watchPosition(
    (pos) => {
      onSuccess({
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
        accuracy: pos.coords.accuracy,
        speed: pos.coords.speed,
        heading: pos.coords.heading,
        timestamp: pos.timestamp,
      });
    },
    (err) => {
      onError({ code: err.code, message: err.message });
    },
    {
      enableHighAccuracy: options.enableHighAccuracy ?? true,
      timeout: options.timeout ?? 15000,
      maximumAge: options.maximumAge ?? 3000,
    },
  );

  return () => {
    navigator.geolocation.clearWatch(watchId);
  };
}

/**
 * Get the current position once.
 */
export async function getCurrentPosition(
  options: GeoWatchOptions = {},
): Promise<GeoPosition> {
  if (isNative()) {
    const geo = await getPlugin();
    const pos = await geo.getCurrentPosition({
      enableHighAccuracy: options.enableHighAccuracy ?? true,
      timeout: options.timeout ?? 10000,
      maximumAge: options.maximumAge ?? 5000,
    });
    return {
      lat: pos.coords.latitude,
      lng: pos.coords.longitude,
      accuracy: pos.coords.accuracy,
      speed: pos.coords.speed,
      heading: pos.coords.heading,
      timestamp: pos.timestamp,
    };
  }

  // Web fallback
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(
      (pos) =>
        resolve({
          lat: pos.coords.latitude,
          lng: pos.coords.longitude,
          accuracy: pos.coords.accuracy,
          speed: pos.coords.speed,
          heading: pos.coords.heading,
          timestamp: pos.timestamp,
        }),
      (err) => reject({ code: err.code, message: err.message }),
      {
        enableHighAccuracy: options.enableHighAccuracy ?? true,
        timeout: options.timeout ?? 10000,
        maximumAge: options.maximumAge ?? 5000,
      },
    );
  });
}

/**
 * Check / request geolocation permission.
 */
export async function checkPermission(): Promise<'granted' | 'denied' | 'prompt'> {
  if (isNative()) {
    const geo = await getPlugin();
    const perm = await geo.checkPermissions();
    if (perm.location === 'granted' || perm.coarseLocation === 'granted') return 'granted';
    if (perm.location === 'denied') return 'denied';
    return 'prompt';
  }

  // Web fallback
  if (!navigator.permissions) return 'prompt';
  const result = await navigator.permissions.query({ name: 'geolocation' as PermissionName });
  return result.state as 'granted' | 'denied' | 'prompt';
}
