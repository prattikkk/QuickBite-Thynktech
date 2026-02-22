/**
 * usePWA — Provides install-prompt handling, online/offline state,
 * and service-worker update awareness for the QuickBite PWA.
 *
 * Uses vite-plugin-pwa's virtual `virtual:pwa-register/react` module
 * for SW lifecycle management. In Capacitor builds, the virtual module
 * is aliased to a no-op stub via vite.config.ts.
 */

import { useCallback, useEffect, useRef, useState } from 'react';
import { useRegisterSW } from 'virtual:pwa-register/react';
import { isNative } from '../native/platform';

/* ── Types ─────────────────────────────────────────────────────── */

export interface PWAState {
  /** An install prompt is available (show "Add to Home Screen" UI). */
  canInstall: boolean;
  /** The app is already running in standalone / installed mode. */
  isInstalled: boolean;
  /** Whether the browser is online. */
  isOnline: boolean;
  /** A new SW version is waiting — call `updateServiceWorker()` to apply. */
  needRefresh: boolean;
  /** True when running inside a Capacitor native shell. */
  isNativeApp: boolean;
  /** Trigger the native install prompt. Resolves with the user choice. */
  promptInstall: () => Promise<'accepted' | 'dismissed' | 'unavailable'>;
  /** Accept the waiting SW and reload. */
  updateServiceWorker: () => void;
  /** Dismiss the "update available" banner without updating. */
  dismissUpdate: () => void;
}

/* ── Hook implementation ───────────────────────────────────────── */

export function usePWA(): PWAState {
  /* ─ Service-worker registration via vite-plugin-pwa ─ */
  const {
    needRefresh: [needRefresh, setNeedRefresh],
    updateServiceWorker,
  } = useRegisterSW({
    onRegisteredSW(swUrl, registration) {
      // Check for updates every 60 minutes
      if (registration) {
        setInterval(() => {
          registration.update();
        }, 60 * 60 * 1000);
      }
      console.log('[PWA] Service worker registered:', swUrl);
    },
    onRegisterError(error) {
      console.error('[PWA] Service worker registration error:', error);
    },
  });

  /* ─ Before-install-prompt handling ─ */
  const deferredPromptRef = useRef<BeforeInstallPromptEvent | null>(null);
  const [canInstall, setCanInstall] = useState(false);

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault();
      deferredPromptRef.current = e as BeforeInstallPromptEvent;
      setCanInstall(true);
    };
    window.addEventListener('beforeinstallprompt', handler);

    // Detect if app was installed while page open
    const installedHandler = () => {
      setCanInstall(false);
      deferredPromptRef.current = null;
    };
    window.addEventListener('appinstalled', installedHandler);

    return () => {
      window.removeEventListener('beforeinstallprompt', handler);
      window.removeEventListener('appinstalled', installedHandler);
    };
  }, []);

  const isInstalled =
    typeof window !== 'undefined' &&
    (window.matchMedia('(display-mode: standalone)').matches ||
      // @ts-expect-error — Safari standalone flag
      window.navigator.standalone === true);

  const promptInstall = useCallback(async (): Promise<
    'accepted' | 'dismissed' | 'unavailable'
  > => {
    if (!deferredPromptRef.current) return 'unavailable';
    deferredPromptRef.current.prompt();
    const { outcome } = await deferredPromptRef.current.userChoice;
    deferredPromptRef.current = null;
    setCanInstall(false);
    return outcome;
  }, []);

  /* ─ Online / Offline ─ */
  const [isOnline, setIsOnline] = useState(
    typeof navigator !== 'undefined' ? navigator.onLine : true,
  );

  useEffect(() => {
    const on = () => setIsOnline(true);
    const off = () => setIsOnline(false);
    window.addEventListener('online', on);
    window.addEventListener('offline', off);
    return () => {
      window.removeEventListener('online', on);
      window.removeEventListener('offline', off);
    };
  }, []);

  /* ─ Dismiss update banner ─ */
  const dismissUpdate = useCallback(() => {
    setNeedRefresh(false);
  }, [setNeedRefresh]);

  return {
    canInstall,
    isInstalled,
    isOnline,
    needRefresh,
    isNativeApp: isNative(),
    promptInstall,
    updateServiceWorker: () => updateServiceWorker(true),
    dismissUpdate,
  };
}

/* ── Web API type augmentation (BeforeInstallPromptEvent) ──────── */

interface BeforeInstallPromptEvent extends Event {
  readonly platforms: string[];
  readonly userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>;
  prompt(): Promise<void>;
}

declare global {
  interface WindowEventMap {
    beforeinstallprompt: BeforeInstallPromptEvent;
  }
}
