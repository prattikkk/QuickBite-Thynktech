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
