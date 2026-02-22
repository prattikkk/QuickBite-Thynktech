/**
 * Capacitor configuration for QuickBite mobile wrapper.
 *
 * The app wraps the Vite-built SPA in a native Android/iOS WebView.
 * All web code is compiled to dist/ by `npm run build`, then
 * synced to native projects via `npx cap sync`.
 *
 * @see https://capacitorjs.com/docs/config
 */
import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.quickbite.driver',
  appName: 'QuickBite Driver',
  webDir: 'dist',

  /* ── Server / origin ───────────────────────────────────────── */
  server: {
    // In production builds the app loads from the bundled dist/ assets.
    // During development, uncomment and set to your dev server URL:
    // url: 'http://192.168.x.x:5173',

    // Allow mixed content during dev (http backend behind https WebView)
    androidScheme: 'https',

    // Required so that cookies / CORS work correctly with the backend
    allowNavigation: ['*.quickbite.com', 'localhost'],
  },

  /* ── Plugin configuration ──────────────────────────────────── */
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      launchAutoHide: true,
      launchFadeOutDuration: 500,
      backgroundColor: '#f97316',       // QuickBite orange
      androidSplashResourceName: 'splash',
      showSpinner: false,
    },

    StatusBar: {
      style: 'LIGHT',                  // light text on coloured bar
      backgroundColor: '#f97316',       // QuickBite orange
    },

    Keyboard: {
      resize: 'body',                   // resize WebView when keyboard opens
      scrollPadding: true,
      style: 'LIGHT',
    },

    PushNotifications: {
      presentationOptions: ['badge', 'sound', 'alert'],
    },

    Camera: {
      // iOS only — photo library usage description
      photoLibraryUsageDescription: 'QuickBite needs access to your photos for proof-of-delivery.',
      cameraUsageDescription: 'QuickBite needs camera access for proof-of-delivery photos.',
    },

    Geolocation: {
      // iOS only (Info.plist descriptions set via native project)
    },
  },

  /* ── Android-specific ──────────────────────────────────────── */
  android: {
    allowMixedContent: true,           // dev: http API calls from https WebView
    backgroundColor: '#ffffff',
  },

  /* ── iOS-specific ──────────────────────────────────────────── */
  ios: {
    contentInset: 'automatic',         // handle safe-area insets
    backgroundColor: '#ffffff',
    scheme: 'QuickBite',
  },
};

export default config;
