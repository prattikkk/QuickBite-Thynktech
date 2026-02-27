// vite.config.ts
import { defineConfig } from "file:///C:/Users/Admin/QuickBite/frontend/node_modules/vite/dist/node/index.js";
import react from "file:///C:/Users/Admin/QuickBite/frontend/node_modules/@vitejs/plugin-react/dist/index.js";
import { VitePWA } from "file:///C:/Users/Admin/QuickBite/frontend/node_modules/vite-plugin-pwa/dist/index.js";
import path from "path";
var __vite_injected_original_dirname = "C:\\Users\\Admin\\QuickBite\\frontend";
var isCapacitor = process.env.VITE_CAPACITOR === "true";
var vite_config_default = defineConfig({
  plugins: [
    react(),
    // Skip PWA plugin entirely for Capacitor native builds
    ...!isCapacitor ? [VitePWA({
      registerType: "prompt",
      includeAssets: ["icons/icon-72x72.svg", "icons/icon-192x192.svg", "icons/icon-512x512.svg"],
      manifest: {
        name: "QuickBite \u2014 Food Delivery",
        short_name: "QuickBite",
        description: "Order food from your favourite restaurants, delivered fast.",
        theme_color: "#f97316",
        background_color: "#ffffff",
        display: "standalone",
        orientation: "portrait-primary",
        scope: "/",
        start_url: "/",
        categories: ["food", "delivery", "shopping"],
        icons: [
          {
            src: "/icons/icon-72x72.svg",
            sizes: "72x72",
            type: "image/svg+xml",
            purpose: "any"
          },
          {
            src: "/icons/icon-192x192.svg",
            sizes: "192x192",
            type: "image/svg+xml",
            purpose: "any maskable"
          },
          {
            src: "/icons/icon-512x512.svg",
            sizes: "512x512",
            type: "image/svg+xml",
            purpose: "any maskable"
          }
        ]
      },
      workbox: {
        // ── Runtime caching strategies ──
        runtimeCaching: [
          // Cache API calls with NetworkFirst (offline-friendly reads)
          {
            urlPattern: /^https?:\/\/.*\/api\/(vendors|orders|drivers|notifications)/,
            handler: "NetworkFirst",
            options: {
              cacheName: "api-cache",
              expiration: { maxEntries: 100, maxAgeSeconds: 5 * 60 },
              networkTimeoutSeconds: 5,
              cacheableResponse: { statuses: [0, 200] }
            }
          },
          // Cache uploaded proof images (immutable)
          {
            urlPattern: /\/uploads\/proofs\//,
            handler: "CacheFirst",
            options: {
              cacheName: "proof-images",
              expiration: { maxEntries: 50, maxAgeSeconds: 7 * 24 * 60 * 60 },
              cacheableResponse: { statuses: [0, 200] }
            }
          },
          // Cache Google Fonts / CDN assets
          {
            urlPattern: /^https:\/\/fonts\.(?:googleapis|gstatic)\.com\/.*/i,
            handler: "CacheFirst",
            options: {
              cacheName: "google-fonts",
              expiration: { maxEntries: 20, maxAgeSeconds: 365 * 24 * 60 * 60 },
              cacheableResponse: { statuses: [0, 200] }
            }
          }
        ],
        // Pre-cache critical navigation shells (auto-detected by Vite build)
        globPatterns: ["**/*.{js,css,html,svg,png,ico,woff2}"],
        // Skip waiting so updates apply on next navigation
        skipWaiting: false,
        clientsClaim: true,
        // Navigation fallback for SPA routing
        navigateFallback: "index.html",
        navigateFallbackDenylist: [/^\/api/, /^\/ws/, /^\/uploads/]
      },
      devOptions: {
        enabled: false
        // enable during PWA development debugging
      }
    })] : []
  ],
  resolve: {
    alias: {
      "@": path.resolve(__vite_injected_original_dirname, "./src"),
      // Redirect the PWA virtual module to a no-op stub in Capacitor builds
      ...isCapacitor ? { "virtual:pwa-register/react": path.resolve(__vite_injected_original_dirname, "src/hooks/pwa-register-stub.ts") } : {}
    }
  },
  envPrefix: "VITE_",
  define: {
    global: "globalThis"
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          // Core vendor libraries
          "vendor-react": ["react", "react-dom", "react-router-dom"],
          // State management + HTTP
          "vendor-data": ["zustand", "axios"],
          // UI utilities
          "vendor-stripe": ["@stripe/stripe-js", "@stripe/react-stripe-js"]
        }
      }
    }
  },
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/ws": {
        target: "ws://localhost:8080",
        ws: true
      }
    }
  },
  preview: {
    port: 4173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/ws": {
        target: "ws://localhost:8080",
        ws: true
      },
      "/uploads": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcudHMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxBZG1pblxcXFxRdWlja0JpdGVcXFxcZnJvbnRlbmRcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXEFkbWluXFxcXFF1aWNrQml0ZVxcXFxmcm9udGVuZFxcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vQzovVXNlcnMvQWRtaW4vUXVpY2tCaXRlL2Zyb250ZW5kL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcclxuaW1wb3J0IHJlYWN0IGZyb20gJ0B2aXRlanMvcGx1Z2luLXJlYWN0J1xyXG5pbXBvcnQgeyBWaXRlUFdBIH0gZnJvbSAndml0ZS1wbHVnaW4tcHdhJ1xyXG5pbXBvcnQgcGF0aCBmcm9tICdwYXRoJ1xyXG5cclxuLy8gQ2FwYWNpdG9yIGJ1aWxkcyBzaG91bGQgTk9UIHJlZ2lzdGVyIGEgc2VydmljZSB3b3JrZXIgKG5hdGl2ZSBzaGVsbCBoYW5kbGVzIGNhY2hpbmcpXHJcbmNvbnN0IGlzQ2FwYWNpdG9yID0gcHJvY2Vzcy5lbnYuVklURV9DQVBBQ0lUT1IgPT09ICd0cnVlJztcclxuXHJcbi8vIGh0dHBzOi8vdml0ZWpzLmRldi9jb25maWcvXHJcbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XHJcbiAgcGx1Z2luczogW1xyXG4gICAgcmVhY3QoKSxcclxuICAgIC8vIFNraXAgUFdBIHBsdWdpbiBlbnRpcmVseSBmb3IgQ2FwYWNpdG9yIG5hdGl2ZSBidWlsZHNcclxuICAgIC4uLighaXNDYXBhY2l0b3IgPyBbVml0ZVBXQSh7XHJcbiAgICAgIHJlZ2lzdGVyVHlwZTogJ3Byb21wdCcsXHJcbiAgICAgIGluY2x1ZGVBc3NldHM6IFsnaWNvbnMvaWNvbi03Mng3Mi5zdmcnLCAnaWNvbnMvaWNvbi0xOTJ4MTkyLnN2ZycsICdpY29ucy9pY29uLTUxMng1MTIuc3ZnJ10sXHJcbiAgICAgIG1hbmlmZXN0OiB7XHJcbiAgICAgICAgbmFtZTogJ1F1aWNrQml0ZSBcdTIwMTQgRm9vZCBEZWxpdmVyeScsXHJcbiAgICAgICAgc2hvcnRfbmFtZTogJ1F1aWNrQml0ZScsXHJcbiAgICAgICAgZGVzY3JpcHRpb246ICdPcmRlciBmb29kIGZyb20geW91ciBmYXZvdXJpdGUgcmVzdGF1cmFudHMsIGRlbGl2ZXJlZCBmYXN0LicsXHJcbiAgICAgICAgdGhlbWVfY29sb3I6ICcjZjk3MzE2JyxcclxuICAgICAgICBiYWNrZ3JvdW5kX2NvbG9yOiAnI2ZmZmZmZicsXHJcbiAgICAgICAgZGlzcGxheTogJ3N0YW5kYWxvbmUnLFxyXG4gICAgICAgIG9yaWVudGF0aW9uOiAncG9ydHJhaXQtcHJpbWFyeScsXHJcbiAgICAgICAgc2NvcGU6ICcvJyxcclxuICAgICAgICBzdGFydF91cmw6ICcvJyxcclxuICAgICAgICBjYXRlZ29yaWVzOiBbJ2Zvb2QnLCAnZGVsaXZlcnknLCAnc2hvcHBpbmcnXSxcclxuICAgICAgICBpY29uczogW1xyXG4gICAgICAgICAge1xyXG4gICAgICAgICAgICBzcmM6ICcvaWNvbnMvaWNvbi03Mng3Mi5zdmcnLFxyXG4gICAgICAgICAgICBzaXplczogJzcyeDcyJyxcclxuICAgICAgICAgICAgdHlwZTogJ2ltYWdlL3N2Zyt4bWwnLFxyXG4gICAgICAgICAgICBwdXJwb3NlOiAnYW55JyxcclxuICAgICAgICAgIH0sXHJcbiAgICAgICAgICB7XHJcbiAgICAgICAgICAgIHNyYzogJy9pY29ucy9pY29uLTE5MngxOTIuc3ZnJyxcclxuICAgICAgICAgICAgc2l6ZXM6ICcxOTJ4MTkyJyxcclxuICAgICAgICAgICAgdHlwZTogJ2ltYWdlL3N2Zyt4bWwnLFxyXG4gICAgICAgICAgICBwdXJwb3NlOiAnYW55IG1hc2thYmxlJyxcclxuICAgICAgICAgIH0sXHJcbiAgICAgICAgICB7XHJcbiAgICAgICAgICAgIHNyYzogJy9pY29ucy9pY29uLTUxMng1MTIuc3ZnJyxcclxuICAgICAgICAgICAgc2l6ZXM6ICc1MTJ4NTEyJyxcclxuICAgICAgICAgICAgdHlwZTogJ2ltYWdlL3N2Zyt4bWwnLFxyXG4gICAgICAgICAgICBwdXJwb3NlOiAnYW55IG1hc2thYmxlJyxcclxuICAgICAgICAgIH0sXHJcbiAgICAgICAgXSxcclxuICAgICAgfSxcclxuICAgICAgd29ya2JveDoge1xyXG4gICAgICAgIC8vIFx1MjUwMFx1MjUwMCBSdW50aW1lIGNhY2hpbmcgc3RyYXRlZ2llcyBcdTI1MDBcdTI1MDBcclxuICAgICAgICBydW50aW1lQ2FjaGluZzogW1xyXG4gICAgICAgICAgLy8gQ2FjaGUgQVBJIGNhbGxzIHdpdGggTmV0d29ya0ZpcnN0IChvZmZsaW5lLWZyaWVuZGx5IHJlYWRzKVxyXG4gICAgICAgICAge1xyXG4gICAgICAgICAgICB1cmxQYXR0ZXJuOiAvXmh0dHBzPzpcXC9cXC8uKlxcL2FwaVxcLyh2ZW5kb3JzfG9yZGVyc3xkcml2ZXJzfG5vdGlmaWNhdGlvbnMpLyxcclxuICAgICAgICAgICAgaGFuZGxlcjogJ05ldHdvcmtGaXJzdCcsXHJcbiAgICAgICAgICAgIG9wdGlvbnM6IHtcclxuICAgICAgICAgICAgICBjYWNoZU5hbWU6ICdhcGktY2FjaGUnLFxyXG4gICAgICAgICAgICAgIGV4cGlyYXRpb246IHsgbWF4RW50cmllczogMTAwLCBtYXhBZ2VTZWNvbmRzOiA1ICogNjAgfSxcclxuICAgICAgICAgICAgICBuZXR3b3JrVGltZW91dFNlY29uZHM6IDUsXHJcbiAgICAgICAgICAgICAgY2FjaGVhYmxlUmVzcG9uc2U6IHsgc3RhdHVzZXM6IFswLCAyMDBdIH0sXHJcbiAgICAgICAgICAgIH0sXHJcbiAgICAgICAgICB9LFxyXG4gICAgICAgICAgLy8gQ2FjaGUgdXBsb2FkZWQgcHJvb2YgaW1hZ2VzIChpbW11dGFibGUpXHJcbiAgICAgICAgICB7XHJcbiAgICAgICAgICAgIHVybFBhdHRlcm46IC9cXC91cGxvYWRzXFwvcHJvb2ZzXFwvLyxcclxuICAgICAgICAgICAgaGFuZGxlcjogJ0NhY2hlRmlyc3QnLFxyXG4gICAgICAgICAgICBvcHRpb25zOiB7XHJcbiAgICAgICAgICAgICAgY2FjaGVOYW1lOiAncHJvb2YtaW1hZ2VzJyxcclxuICAgICAgICAgICAgICBleHBpcmF0aW9uOiB7IG1heEVudHJpZXM6IDUwLCBtYXhBZ2VTZWNvbmRzOiA3ICogMjQgKiA2MCAqIDYwIH0sXHJcbiAgICAgICAgICAgICAgY2FjaGVhYmxlUmVzcG9uc2U6IHsgc3RhdHVzZXM6IFswLCAyMDBdIH0sXHJcbiAgICAgICAgICAgIH0sXHJcbiAgICAgICAgICB9LFxyXG4gICAgICAgICAgLy8gQ2FjaGUgR29vZ2xlIEZvbnRzIC8gQ0ROIGFzc2V0c1xyXG4gICAgICAgICAge1xyXG4gICAgICAgICAgICB1cmxQYXR0ZXJuOiAvXmh0dHBzOlxcL1xcL2ZvbnRzXFwuKD86Z29vZ2xlYXBpc3xnc3RhdGljKVxcLmNvbVxcLy4qL2ksXHJcbiAgICAgICAgICAgIGhhbmRsZXI6ICdDYWNoZUZpcnN0JyxcclxuICAgICAgICAgICAgb3B0aW9uczoge1xyXG4gICAgICAgICAgICAgIGNhY2hlTmFtZTogJ2dvb2dsZS1mb250cycsXHJcbiAgICAgICAgICAgICAgZXhwaXJhdGlvbjogeyBtYXhFbnRyaWVzOiAyMCwgbWF4QWdlU2Vjb25kczogMzY1ICogMjQgKiA2MCAqIDYwIH0sXHJcbiAgICAgICAgICAgICAgY2FjaGVhYmxlUmVzcG9uc2U6IHsgc3RhdHVzZXM6IFswLCAyMDBdIH0sXHJcbiAgICAgICAgICAgIH0sXHJcbiAgICAgICAgICB9LFxyXG4gICAgICAgIF0sXHJcbiAgICAgICAgLy8gUHJlLWNhY2hlIGNyaXRpY2FsIG5hdmlnYXRpb24gc2hlbGxzIChhdXRvLWRldGVjdGVkIGJ5IFZpdGUgYnVpbGQpXHJcbiAgICAgICAgZ2xvYlBhdHRlcm5zOiBbJyoqLyoue2pzLGNzcyxodG1sLHN2ZyxwbmcsaWNvLHdvZmYyfSddLFxyXG4gICAgICAgIC8vIFNraXAgd2FpdGluZyBzbyB1cGRhdGVzIGFwcGx5IG9uIG5leHQgbmF2aWdhdGlvblxyXG4gICAgICAgIHNraXBXYWl0aW5nOiBmYWxzZSxcclxuICAgICAgICBjbGllbnRzQ2xhaW06IHRydWUsXHJcbiAgICAgICAgLy8gTmF2aWdhdGlvbiBmYWxsYmFjayBmb3IgU1BBIHJvdXRpbmdcclxuICAgICAgICBuYXZpZ2F0ZUZhbGxiYWNrOiAnaW5kZXguaHRtbCcsXHJcbiAgICAgICAgbmF2aWdhdGVGYWxsYmFja0RlbnlsaXN0OiBbL15cXC9hcGkvLCAvXlxcL3dzLywgL15cXC91cGxvYWRzL10sXHJcbiAgICAgIH0sXHJcbiAgICAgIGRldk9wdGlvbnM6IHtcclxuICAgICAgICBlbmFibGVkOiBmYWxzZSwgLy8gZW5hYmxlIGR1cmluZyBQV0EgZGV2ZWxvcG1lbnQgZGVidWdnaW5nXHJcbiAgICAgIH0sXHJcbiAgICB9KV0gOiBbXSksXHJcbiAgXSxcclxuICByZXNvbHZlOiB7XHJcbiAgICBhbGlhczoge1xyXG4gICAgICAnQCc6IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsICcuL3NyYycpLFxyXG4gICAgICAvLyBSZWRpcmVjdCB0aGUgUFdBIHZpcnR1YWwgbW9kdWxlIHRvIGEgbm8tb3Agc3R1YiBpbiBDYXBhY2l0b3IgYnVpbGRzXHJcbiAgICAgIC4uLihpc0NhcGFjaXRvclxyXG4gICAgICAgID8geyAndmlydHVhbDpwd2EtcmVnaXN0ZXIvcmVhY3QnOiBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnc3JjL2hvb2tzL3B3YS1yZWdpc3Rlci1zdHViLnRzJykgfVxyXG4gICAgICAgIDoge30pLFxyXG4gICAgfSxcclxuICB9LFxyXG4gIGVudlByZWZpeDogJ1ZJVEVfJyxcclxuICBkZWZpbmU6IHtcclxuICAgIGdsb2JhbDogJ2dsb2JhbFRoaXMnLFxyXG4gIH0sXHJcbiAgYnVpbGQ6IHtcclxuICAgIHJvbGx1cE9wdGlvbnM6IHtcclxuICAgICAgb3V0cHV0OiB7XHJcbiAgICAgICAgbWFudWFsQ2h1bmtzOiB7XHJcbiAgICAgICAgICAvLyBDb3JlIHZlbmRvciBsaWJyYXJpZXNcclxuICAgICAgICAgICd2ZW5kb3ItcmVhY3QnOiBbJ3JlYWN0JywgJ3JlYWN0LWRvbScsICdyZWFjdC1yb3V0ZXItZG9tJ10sXHJcbiAgICAgICAgICAvLyBTdGF0ZSBtYW5hZ2VtZW50ICsgSFRUUFxyXG4gICAgICAgICAgJ3ZlbmRvci1kYXRhJzogWyd6dXN0YW5kJywgJ2F4aW9zJ10sXHJcbiAgICAgICAgICAvLyBVSSB1dGlsaXRpZXNcclxuICAgICAgICAgICd2ZW5kb3Itc3RyaXBlJzogWydAc3RyaXBlL3N0cmlwZS1qcycsICdAc3RyaXBlL3JlYWN0LXN0cmlwZS1qcyddLFxyXG4gICAgICAgIH0sXHJcbiAgICAgIH0sXHJcbiAgICB9LFxyXG4gIH0sXHJcbiAgc2VydmVyOiB7XHJcbiAgICBwb3J0OiA1MTczLFxyXG4gICAgcHJveHk6IHtcclxuICAgICAgJy9hcGknOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgwJyxcclxuICAgICAgICBjaGFuZ2VPcmlnaW46IHRydWUsXHJcbiAgICAgIH0sXHJcbiAgICAgICcvd3MnOiB7XHJcbiAgICAgICAgdGFyZ2V0OiAnd3M6Ly9sb2NhbGhvc3Q6ODA4MCcsXHJcbiAgICAgICAgd3M6IHRydWUsXHJcbiAgICAgIH1cclxuICAgIH1cclxuICB9LFxyXG4gIHByZXZpZXc6IHtcclxuICAgIHBvcnQ6IDQxNzMsXHJcbiAgICBwcm94eToge1xyXG4gICAgICAnL2FwaSc6IHtcclxuICAgICAgICB0YXJnZXQ6ICdodHRwOi8vbG9jYWxob3N0OjgwODAnLFxyXG4gICAgICAgIGNoYW5nZU9yaWdpbjogdHJ1ZSxcclxuICAgICAgfSxcclxuICAgICAgJy93cyc6IHtcclxuICAgICAgICB0YXJnZXQ6ICd3czovL2xvY2FsaG9zdDo4MDgwJyxcclxuICAgICAgICB3czogdHJ1ZSxcclxuICAgICAgfSxcclxuICAgICAgJy91cGxvYWRzJzoge1xyXG4gICAgICAgIHRhcmdldDogJ2h0dHA6Ly9sb2NhbGhvc3Q6ODA4MCcsXHJcbiAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlLFxyXG4gICAgICB9XHJcbiAgICB9XHJcbiAgfVxyXG59KVxyXG4iXSwKICAibWFwcGluZ3MiOiAiO0FBQStSLFNBQVMsb0JBQW9CO0FBQzVULE9BQU8sV0FBVztBQUNsQixTQUFTLGVBQWU7QUFDeEIsT0FBTyxVQUFVO0FBSGpCLElBQU0sbUNBQW1DO0FBTXpDLElBQU0sY0FBYyxRQUFRLElBQUksbUJBQW1CO0FBR25ELElBQU8sc0JBQVEsYUFBYTtBQUFBLEVBQzFCLFNBQVM7QUFBQSxJQUNQLE1BQU07QUFBQTtBQUFBLElBRU4sR0FBSSxDQUFDLGNBQWMsQ0FBQyxRQUFRO0FBQUEsTUFDMUIsY0FBYztBQUFBLE1BQ2QsZUFBZSxDQUFDLHdCQUF3QiwwQkFBMEIsd0JBQXdCO0FBQUEsTUFDMUYsVUFBVTtBQUFBLFFBQ1IsTUFBTTtBQUFBLFFBQ04sWUFBWTtBQUFBLFFBQ1osYUFBYTtBQUFBLFFBQ2IsYUFBYTtBQUFBLFFBQ2Isa0JBQWtCO0FBQUEsUUFDbEIsU0FBUztBQUFBLFFBQ1QsYUFBYTtBQUFBLFFBQ2IsT0FBTztBQUFBLFFBQ1AsV0FBVztBQUFBLFFBQ1gsWUFBWSxDQUFDLFFBQVEsWUFBWSxVQUFVO0FBQUEsUUFDM0MsT0FBTztBQUFBLFVBQ0w7QUFBQSxZQUNFLEtBQUs7QUFBQSxZQUNMLE9BQU87QUFBQSxZQUNQLE1BQU07QUFBQSxZQUNOLFNBQVM7QUFBQSxVQUNYO0FBQUEsVUFDQTtBQUFBLFlBQ0UsS0FBSztBQUFBLFlBQ0wsT0FBTztBQUFBLFlBQ1AsTUFBTTtBQUFBLFlBQ04sU0FBUztBQUFBLFVBQ1g7QUFBQSxVQUNBO0FBQUEs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
