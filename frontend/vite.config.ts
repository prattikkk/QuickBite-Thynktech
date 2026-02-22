import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { VitePWA } from 'vite-plugin-pwa'
import path from 'path'

// Capacitor builds should NOT register a service worker (native shell handles caching)
const isCapacitor = process.env.VITE_CAPACITOR === 'true';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    react(),
    // Skip PWA plugin entirely for Capacitor native builds
    ...(!isCapacitor ? [VitePWA({
      registerType: 'prompt',
      includeAssets: ['icons/icon-72x72.svg', 'icons/icon-192x192.svg', 'icons/icon-512x512.svg'],
      manifest: {
        name: 'QuickBite — Food Delivery',
        short_name: 'QuickBite',
        description: 'Order food from your favourite restaurants, delivered fast.',
        theme_color: '#f97316',
        background_color: '#ffffff',
        display: 'standalone',
        orientation: 'portrait-primary',
        scope: '/',
        start_url: '/',
        categories: ['food', 'delivery', 'shopping'],
        icons: [
          {
            src: '/icons/icon-72x72.svg',
            sizes: '72x72',
            type: 'image/svg+xml',
            purpose: 'any',
          },
          {
            src: '/icons/icon-192x192.svg',
            sizes: '192x192',
            type: 'image/svg+xml',
            purpose: 'any maskable',
          },
          {
            src: '/icons/icon-512x512.svg',
            sizes: '512x512',
            type: 'image/svg+xml',
            purpose: 'any maskable',
          },
        ],
      },
      workbox: {
        // ── Runtime caching strategies ──
        runtimeCaching: [
          // Cache API calls with NetworkFirst (offline-friendly reads)
          {
            urlPattern: /^https?:\/\/.*\/api\/(vendors|orders|drivers|notifications)/,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: { maxEntries: 100, maxAgeSeconds: 5 * 60 },
              networkTimeoutSeconds: 5,
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          // Cache uploaded proof images (immutable)
          {
            urlPattern: /\/uploads\/proofs\//,
            handler: 'CacheFirst',
            options: {
              cacheName: 'proof-images',
              expiration: { maxEntries: 50, maxAgeSeconds: 7 * 24 * 60 * 60 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
          // Cache Google Fonts / CDN assets
          {
            urlPattern: /^https:\/\/fonts\.(?:googleapis|gstatic)\.com\/.*/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'google-fonts',
              expiration: { maxEntries: 20, maxAgeSeconds: 365 * 24 * 60 * 60 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
        // Pre-cache critical navigation shells (auto-detected by Vite build)
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        // Skip waiting so updates apply on next navigation
        skipWaiting: false,
        clientsClaim: true,
        // Navigation fallback for SPA routing
        navigateFallback: 'index.html',
        navigateFallbackDenylist: [/^\/api/, /^\/ws/, /^\/uploads/],
      },
      devOptions: {
        enabled: false, // enable during PWA development debugging
      },
    })] : []),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      // Redirect the PWA virtual module to a no-op stub in Capacitor builds
      ...(isCapacitor
        ? { 'virtual:pwa-register/react': path.resolve(__dirname, 'src/hooks/pwa-register-stub.ts') }
        : {}),
    },
  },
  envPrefix: 'VITE_',
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      }
    }
  },
  preview: {
    port: 4173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
