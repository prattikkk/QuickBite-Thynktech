/**
 * PWAInstallPrompt — A banner / bottom-sheet that invites the user
 * to install QuickBite as a standalone app.
 *
 * Renders only when the browser fires `beforeinstallprompt`
 * (Chrome, Edge, Samsung Internet, Opera, WebView-based browsers).
 * Also shows an update-available refresh banner when a new SW is waiting.
 */

import { useState } from 'react';
import { usePWA } from '../hooks/usePWA';

export default function PWAInstallPrompt() {
  const {
    canInstall,
    isInstalled,
    needRefresh,
    promptInstall,
    updateServiceWorker,
    dismissUpdate,
  } = usePWA();

  const [dismissed, setDismissed] = useState(false);

  /* ── Update-available banner ─────────────────────────────────── */
  if (needRefresh) {
    return (
      <div className="fixed bottom-0 inset-x-0 z-50 p-4 sm:p-6 pointer-events-none">
        <div className="max-w-md mx-auto bg-blue-600 text-white rounded-xl shadow-2xl p-4 flex items-center gap-3 pointer-events-auto">
          {/* refresh icon */}
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-6 w-6 shrink-0"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold">New version available</p>
            <p className="text-xs opacity-90">Refresh to get the latest features.</p>
          </div>
          <button
            onClick={() => updateServiceWorker()}
            className="px-3 py-1.5 bg-white text-blue-700 rounded-lg text-sm font-medium hover:bg-blue-50 transition"
          >
            Refresh
          </button>
          <button
            onClick={dismissUpdate}
            className="text-white/70 hover:text-white transition"
            aria-label="Dismiss"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      </div>
    );
  }

  /* ── Install prompt ──────────────────────────────────────────── */
  if (!canInstall || isInstalled || dismissed) return null;

  return (
    <div className="fixed bottom-0 inset-x-0 z-50 p-4 sm:p-6 pointer-events-none">
      <div className="max-w-md mx-auto bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-xl shadow-2xl p-4 flex items-center gap-3 pointer-events-auto">
        {/* app icon */}
        <div className="shrink-0 w-12 h-12 rounded-lg bg-white/20 flex items-center justify-center text-xl font-bold">
          QB
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold">Install QuickBite</p>
          <p className="text-xs opacity-90">
            Add to home screen for the best experience.
          </p>
        </div>
        <button
          onClick={async () => {
            const result = await promptInstall();
            if (result === 'dismissed') setDismissed(true);
          }}
          className="px-3 py-1.5 bg-white text-orange-600 rounded-lg text-sm font-medium hover:bg-orange-50 transition shrink-0"
        >
          Install
        </button>
        <button
          onClick={() => setDismissed(true)}
          className="text-white/70 hover:text-white transition"
          aria-label="Dismiss"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  );
}
