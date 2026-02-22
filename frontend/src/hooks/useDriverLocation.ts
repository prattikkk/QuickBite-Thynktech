/**
 * useDriverLocation hook — Phase 2: Foreground Live Location (PWA-based)
 *
 * Uses navigator.geolocation.watchPosition() to stream the driver's GPS
 * to the backend at ~5 s intervals. Handles consent, accuracy thresholds,
 * and automatic cleanup.
 */

import { useEffect, useRef, useState, useCallback } from 'react';
import { driverService } from '../services/driver.service';

export interface LocationState {
  lat: number | null;
  lng: number | null;
  accuracy: number | null;
  speed: number | null;
  heading: number | null;
  timestamp: number | null;
  error: string | null;
  isTracking: boolean;
  permissionState: 'prompt' | 'granted' | 'denied' | 'unknown';
}

interface UseDriverLocationOptions {
  /** Whether to actively track/send location (controlled by shift state) */
  enabled?: boolean;
  /** Minimum interval between API posts in ms (default: 5000) */
  sendInterval?: number;
  /** Maximum acceptable accuracy in meters before skipping a sample */
  maxAccuracy?: number;
}

const DEFAULT_SEND_INTERVAL = 5000;
const DEFAULT_MAX_ACCURACY = 100; // metres

export const useDriverLocation = ({
  enabled = false,
  sendInterval = DEFAULT_SEND_INTERVAL,
  maxAccuracy = DEFAULT_MAX_ACCURACY,
}: UseDriverLocationOptions = {}) => {
  const [location, setLocation] = useState<LocationState>({
    lat: null,
    lng: null,
    accuracy: null,
    speed: null,
    heading: null,
    timestamp: null,
    error: null,
    isTracking: false,
    permissionState: 'unknown',
  });

  const watchIdRef = useRef<number | null>(null);
  const lastSentRef = useRef<number>(0);
  const enabledRef = useRef(enabled);
  enabledRef.current = enabled;

  // Check / query permission status
  useEffect(() => {
    if (!navigator.permissions) return;
    navigator.permissions
      .query({ name: 'geolocation' as PermissionName })
      .then((result) => {
        setLocation((prev) => ({ ...prev, permissionState: result.state as LocationState['permissionState'] }));
        result.onchange = () => {
          setLocation((prev) => ({ ...prev, permissionState: result.state as LocationState['permissionState'] }));
        };
      })
      .catch(() => {
        // Permissions API not supported — will find out when we try watchPosition
      });
  }, []);

  // Post to backend, throttled by sendInterval
  const sendLocation = useCallback(
    async (pos: GeolocationPosition) => {
      const now = Date.now();
      if (now - lastSentRef.current < sendInterval) return;
      if (pos.coords.accuracy > maxAccuracy) return;

      lastSentRef.current = now;

      try {
        await driverService.updateLocation(
          pos.coords.latitude,
          pos.coords.longitude,
          pos.coords.accuracy,
          pos.coords.speed,
          pos.coords.heading,
        );
      } catch (err) {
        console.error('[useDriverLocation] Failed to post location', err);
      }
    },
    [sendInterval, maxAccuracy],
  );

  // Start / stop watchPosition based on enabled flag
  useEffect(() => {
    if (!enabled) {
      // Stop tracking
      if (watchIdRef.current !== null) {
        navigator.geolocation.clearWatch(watchIdRef.current);
        watchIdRef.current = null;
      }
      setLocation((prev) => ({ ...prev, isTracking: false }));
      return;
    }

    if (!('geolocation' in navigator)) {
      setLocation((prev) => ({
        ...prev,
        error: 'Geolocation not supported by this browser',
        isTracking: false,
      }));
      return;
    }

    const onSuccess = (pos: GeolocationPosition) => {
      setLocation((prev) => ({
        ...prev,
        lat: pos.coords.latitude,
        lng: pos.coords.longitude,
        accuracy: pos.coords.accuracy,
        speed: pos.coords.speed,
        heading: pos.coords.heading,
        timestamp: pos.timestamp,
        error: null,
        isTracking: true,
        permissionState: 'granted',
      }));

      // Post to backend (throttled)
      if (enabledRef.current) {
        sendLocation(pos);
      }
    };

    const onError = (err: GeolocationPositionError) => {
      let errorMsg: string;
      switch (err.code) {
        case err.PERMISSION_DENIED:
          errorMsg = 'Location permission denied';
          break;
        case err.POSITION_UNAVAILABLE:
          errorMsg = 'Position unavailable';
          break;
        case err.TIMEOUT:
          errorMsg = 'Location request timed out';
          break;
        default:
          errorMsg = 'Unknown geolocation error';
      }
      setLocation((prev) => ({
        ...prev,
        error: errorMsg,
        isTracking: false,
        permissionState: err.code === err.PERMISSION_DENIED ? 'denied' : prev.permissionState,
      }));
    };

    const id = navigator.geolocation.watchPosition(onSuccess, onError, {
      enableHighAccuracy: true,
      timeout: 15000,
      maximumAge: 3000,
    });
    watchIdRef.current = id;
    setLocation((prev) => ({ ...prev, isTracking: true, error: null }));

    return () => {
      navigator.geolocation.clearWatch(id);
      watchIdRef.current = null;
    };
  }, [enabled, sendLocation]);

  return location;
};

export default useDriverLocation;
