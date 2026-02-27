import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.css'
import { initNativeApp } from './native/app-lifecycle'
import { initPushNotifications } from './native/push'
import { isNative } from './native/platform'
import { initSentry } from './services/sentry'

// Restore dark mode preference before first paint to avoid flash
(() => {
  const theme = localStorage.getItem('theme');
  if (theme === 'dark' || (!theme && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
  } else {
    document.documentElement.classList.remove('dark');
  }
})();

// In dev mode, unregister any stale service workers from previous production builds
if (import.meta.env.DEV && 'serviceWorker' in navigator) {
  navigator.serviceWorker.getRegistrations().then(registrations => {
    registrations.forEach(r => {
      r.unregister().then(() => console.log('[DEV] Unregistered stale SW:', r.scope));
    });
    if (registrations.length > 0) {
      // Clear caches left behind by the old SW
      caches.keys().then(keys => keys.forEach(k => caches.delete(k)));
    }
  });
}

// Initialize Sentry error monitoring (no-op if VITE_SENTRY_DSN not set)
initSentry().catch(console.error);

// Initialize native plugins when running inside Capacitor shell
if (isNative()) {
  initNativeApp().catch(console.error);
  initPushNotifications().catch(console.error);
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
