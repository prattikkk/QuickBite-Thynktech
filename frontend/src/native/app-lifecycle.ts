/**
 * App lifecycle integration for Capacitor.
 *
 * Handles Android back-button, app state (foreground/background),
 * deep-link URL opens, and status bar / splash screen setup.
 */

import { isNative } from './platform';

type AppStateListener = (isActive: boolean) => void;
type BackButtonListener = () => boolean; // return true to prevent default (exit)
type DeepLinkListener = (url: string) => void;

const _appStateListeners: AppStateListener[] = [];
const _backButtonListeners: BackButtonListener[] = [];
const _deepLinkListeners: DeepLinkListener[] = [];

let _initialized = false;

/**
 * Initialize all native app lifecycle plugins.
 * Call once at app startup (e.g., main.tsx or App.tsx useEffect).
 */
export async function initNativeApp(): Promise<void> {
  if (_initialized || !isNative()) return;
  _initialized = true;

  // ── App plugin (lifecycle, back button, deep links) ───────
  const { App } = await import('@capacitor/app');

  App.addListener('appStateChange', ({ isActive }) => {
    _appStateListeners.forEach((fn) => fn(isActive));
  });

  App.addListener('backButton', () => {
    // Let registered handlers try to handle it first
    const handled = _backButtonListeners.some((fn) => fn());
    if (!handled) {
      // Default: go back in browser history, or exit if at root
      if (window.history.length > 1) {
        window.history.back();
      } else {
        App.exitApp();
      }
    }
  });

  App.addListener('appUrlOpen', ({ url }) => {
    _deepLinkListeners.forEach((fn) => fn(url));
  });

  // ── Status Bar ────────────────────────────────────────────
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar');
    await StatusBar.setStyle({ style: Style.Light });
    await StatusBar.setBackgroundColor({ color: '#f97316' });
  } catch (err) {
    console.warn('[Native] StatusBar setup failed:', err);
  }

  // ── Splash Screen — auto-hide after content loaded ────────
  try {
    const { SplashScreen } = await import('@capacitor/splash-screen');
    await SplashScreen.hide({ fadeOutDuration: 500 });
  } catch (err) {
    console.warn('[Native] SplashScreen hide failed:', err);
  }

  // ── Keyboard — configure resize behavior ──────────────────
  try {
    const { Keyboard } = await import('@capacitor/keyboard');
    // On Android, resize the webview when keyboard opens
    Keyboard.addListener('keyboardWillShow', () => {
      document.body.classList.add('keyboard-visible');
    });
    Keyboard.addListener('keyboardWillHide', () => {
      document.body.classList.remove('keyboard-visible');
    });
  } catch (err) {
    console.warn('[Native] Keyboard setup failed:', err);
  }
}

/* ── Listener registration ───────────────────────────────────── */

export function onAppStateChange(fn: AppStateListener): () => void {
  _appStateListeners.push(fn);
  return () => {
    const idx = _appStateListeners.indexOf(fn);
    if (idx >= 0) _appStateListeners.splice(idx, 1);
  };
}

export function onBackButton(fn: BackButtonListener): () => void {
  _backButtonListeners.push(fn);
  return () => {
    const idx = _backButtonListeners.indexOf(fn);
    if (idx >= 0) _backButtonListeners.splice(idx, 1);
  };
}

export function onDeepLink(fn: DeepLinkListener): () => void {
  _deepLinkListeners.push(fn);
  return () => {
    const idx = _deepLinkListeners.indexOf(fn);
    if (idx >= 0) _deepLinkListeners.splice(idx, 1);
  };
}
