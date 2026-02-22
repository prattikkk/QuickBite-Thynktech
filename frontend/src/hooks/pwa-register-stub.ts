/**
 * Stub for `virtual:pwa-register/react` used in Capacitor builds
 * where the VitePWA plugin is disabled.
 *
 * Exports a no-op useRegisterSW hook so usePWA.ts compiles
 * without the real virtual module.
 */

import { useState, useCallback } from 'react';

type SetStateFn = (v: boolean) => void;

interface RegisterSWOptions {
  onRegisteredSW?: (url: string, registration?: ServiceWorkerRegistration) => void;
  onRegisterError?: (error: Error) => void;
  onNeedRefresh?: () => void;
  onOfflineReady?: () => void;
  immediate?: boolean;
}

export function useRegisterSW(_options?: RegisterSWOptions) {
  const [needRefresh, setNeedRefresh] = useState(false);
  const [offlineReady, setOfflineReady] = useState(false);

  const updateServiceWorker = useCallback((_reloadPage?: boolean) => {
    // no-op on Capacitor
  }, []);

  return {
    needRefresh: [needRefresh, setNeedRefresh] as [boolean, SetStateFn],
    offlineReady: [offlineReady, setOfflineReady] as [boolean, SetStateFn],
    updateServiceWorker,
  };
}
