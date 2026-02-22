/**
 * Native Network plugin wrapper.
 *
 * Uses @capacitor/network on native platforms for reliable connectivity
 * detection. Falls back to navigator.onLine + online/offline events on web.
 */

import { isNative } from './platform';

/* ── Types ───────────────────────────────────────────────────── */

export interface NetworkStatus {
  connected: boolean;
  connectionType: 'wifi' | 'cellular' | 'none' | 'unknown';
}

export type NetworkStatusListener = (status: NetworkStatus) => void;

/* ── Implementation ──────────────────────────────────────────── */

let _plugin: typeof import('@capacitor/network').Network | null = null;

async function getPlugin() {
  if (!_plugin) {
    const mod = await import('@capacitor/network');
    _plugin = mod.Network;
  }
  return _plugin;
}

/**
 * Get the current network status.
 */
export async function getNetworkStatus(): Promise<NetworkStatus> {
  if (isNative()) {
    const net = await getPlugin();
    const status = await net.getStatus();
    return {
      connected: status.connected,
      connectionType: status.connectionType as NetworkStatus['connectionType'],
    };
  }

  return {
    connected: navigator.onLine,
    connectionType: navigator.onLine ? 'unknown' : 'none',
  };
}

/**
 * Listen for network status changes.
 * Returns a disposer function.
 */
export async function onNetworkStatusChange(
  listener: NetworkStatusListener,
): Promise<() => void> {
  if (isNative()) {
    const net = await getPlugin();
    const handle = await net.addListener('networkStatusChange', (status) => {
      listener({
        connected: status.connected,
        connectionType: status.connectionType as NetworkStatus['connectionType'],
      });
    });
    return () => handle.remove();
  }

  // Web fallback
  const onOnline = () => listener({ connected: true, connectionType: 'unknown' });
  const onOffline = () => listener({ connected: false, connectionType: 'none' });
  window.addEventListener('online', onOnline);
  window.addEventListener('offline', onOffline);

  return () => {
    window.removeEventListener('online', onOnline);
    window.removeEventListener('offline', onOffline);
  };
}
