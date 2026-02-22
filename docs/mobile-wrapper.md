# Phase 5 — Mobile Wrapper Readiness

## Overview

QuickBite frontend is now wrapped with **Capacitor 8.x**, enabling native Android and iOS builds from the same React + Vite codebase. The PWA and native app share 100% of the UI code; native-specific behaviours are abstracted through a plugin wrapper layer in `src/native/`.

---

## Architecture

```
┌─────────────────────────────────────┐
│         React + Vite SPA            │
│     (shared 100% of UI code)        │
├─────────────────────────────────────┤
│   src/native/ — Plugin Abstraction  │
│  (Capacitor plugins ↔ Web APIs)     │
├──────────┬─────────┬────────────────┤
│  PWA     │ Android │  iOS           │
│  (SW +   │ (Cap    │  (Cap          │
│  browser │  WebView│   WebView      │
│  APIs)   │  + JNI) │   + Swift)     │
└──────────┴─────────┴────────────────┘
```

### Key Design Decisions

1. **Lazy-loaded plugins** — Capacitor plugin imports use dynamic `import()` so they're tree-shaken out of web builds.
2. **Conditional PWA** — `VITE_CAPACITOR=true` env var disables `vite-plugin-pwa` for native builds (no service worker inside the WebView).
3. **Platform-aware API URL** — `resolveApiBaseUrl()` returns a relative `/api` on web (proxy) and an absolute URL on native (no proxy available).
4. **Feature-flag controlled** — `isNative()` check gates all Capacitor-specific code paths.

---

## File Inventory

### Capacitor Configuration
| File | Purpose |
|------|---------|
| `capacitor.config.ts` | App ID, name, webDir, plugin config |
| `android/` | Native Android project (Gradle, manifests) |
| `.env.capacitor.example` | Env template for native builds |

### Plugin Wrappers (`src/native/`)
| File | Web Fallback | Native Plugin |
|------|-------------|---------------|
| `platform.ts` | `window.matchMedia`, env vars | `Capacitor.isNativePlatform()` |
| `geolocation.ts` | `navigator.geolocation` | `@capacitor/geolocation` |
| `camera.ts` | `<input type="file" capture>` | `@capacitor/camera` |
| `push.ts` | Web Push / Notification API | `@capacitor/push-notifications` |
| `network.ts` | `navigator.onLine` + events | `@capacitor/network` |
| `haptics.ts` | no-op | `@capacitor/haptics` |
| `app-lifecycle.ts` | no-op | `@capacitor/app`, `@capacitor/status-bar`, `@capacitor/splash-screen`, `@capacitor/keyboard` |
| `index.ts` | Barrel exports | — |

### Modified Files
| File | Change |
|------|--------|
| `vite.config.ts` | Conditional `VitePWA` plugin based on `VITE_CAPACITOR` |
| `src/main.tsx` | Init native plugins on startup |
| `src/services/api.ts` | Uses `resolveApiBaseUrl()` for platform-aware URL |
| `src/hooks/usePWA.ts` | Exposes `isNativeApp` flag |
| `package.json` | Added `cap:*` scripts, cross-env, Capacitor deps |
| `.env` | Added `VITE_CAPACITOR=false` |
| `.gitignore` | Exclude Capacitor build artifacts |

---

## Capacitor Plugins Installed

| Plugin | Version | Purpose |
|--------|---------|---------|
| `@capacitor/core` | 8.1.0 | Runtime core |
| `@capacitor/cli` | 8.1.0 | Build tooling (dev) |
| `@capacitor/android` | 8.1.0 | Android platform |
| `@capacitor/ios` | 8.1.0 | iOS platform |
| `@capacitor/app` | 8.0.1 | Lifecycle, back button, deep links |
| `@capacitor/haptics` | 8.0.0 | Tactile feedback |
| `@capacitor/status-bar` | 8.0.1 | Status bar theme (#f97316) |
| `@capacitor/splash-screen` | 8.0.1 | Native splash screen |
| `@capacitor/keyboard` | 8.0.0 | Keyboard resize behaviour |
| `@capacitor/geolocation` | 8.1.0 | Native GPS (foreground) |
| `@capacitor/camera` | 8.0.1 | Camera/gallery for proof photos |
| `@capacitor/push-notifications` | 8.0.1 | FCM / APNs push |
| `@capacitor/network` | 8.0.1 | Network connectivity |
| `@capacitor/preferences` | 8.0.1 | Native key-value storage |
| `@capacitor/browser` | 8.0.1 | External link opener |
| `@capacitor/local-notifications` | 8.0.1 | Local/offline alerts |

---

## Build & Run

### Prerequisites
- Node 18+, npm 9+
- Android Studio (for Android builds)
- Xcode 15+ (for iOS builds — macOS only)
- JDK 17+ (for Android Gradle)

### Web (unchanged)
```bash
npm run dev          # Dev server with HMR
npm run build        # Production build → dist/
npm run preview      # Preview production build
```

### Android
```bash
# One-command build + sync
npm run cap:build:android

# Open in Android Studio
npm run cap:open:android

# Run on connected device/emulator
npm run cap:run:android
```

### iOS (macOS only)
```bash
npm run cap:build:ios
npm run cap:open:ios
npm run cap:run:ios
```

### Native Build Environment Setup
```bash
# 1. Copy env template
cp .env.capacitor.example .env.local

# 2. Edit .env.local — set VITE_NATIVE_API_URL to your backend
#    e.g., https://api.quickbite.com/api or http://192.168.x.x:8080/api

# 3. Build + sync
npm run cap:build:android
```

---

## Android Permissions

Added to `android/app/src/main/AndroidManifest.xml`:

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | API calls (default) |
| `ACCESS_COARSE_LOCATION` | Approximate GPS |
| `ACCESS_FINE_LOCATION` | Precise GPS for driver tracking |
| `CAMERA` | Proof-of-delivery photos |
| `READ_MEDIA_IMAGES` | Gallery access for proof photos |
| `POST_NOTIFICATIONS` | FCM push notifications |
| `ACCESS_NETWORK_STATE` | Connectivity detection |
| `VIBRATE` | Haptic feedback |

### Background Location (Future)
`ACCESS_BACKGROUND_LOCATION` is commented out. Enabling it requires:
1. Google Play policy declaration (why the app needs it)
2. User consent dialog
3. Play Store review

For now, foreground-only tracking (matching the PWA behaviour) is sufficient.

---

## iOS Limitations & Migration Path

### Current Limitations

| Feature | iOS PWA | iOS Capacitor | Notes |
|---------|---------|---------------|-------|
| **Push Notifications** | ❌ Not supported | ✅ APNs via plugin | Major driver for Capacitor on iOS |
| **Background Location** | ❌ Suspended after ~30s | ⚠️ Limited to ~3 min | Need `UIBackgroundModes: location` |
| **Camera** | ✅ via `<input capture>` | ✅ Better UX via plugin | Plugin gives orientation/quality control |
| **Install to Home Screen** | ✅ "Add to Home Screen" | N/A (App Store) | App Store for native builds |
| **Service Worker** | ✅ (limited cache) | N/A (no SW needed) | — |
| **Haptics** | ❌ No API | ✅ Native haptics | — |

### Background Location on iOS

iOS aggressively kills background processes. Options:

1. **Significant Location Changes** (low power) — receives updates only on ~500m moves. Good for long-distance delivery tracking but not ETA precision.
2. **`UIBackgroundModes: location`** — allows continuous tracking but:
   - Requires "Always" location permission (user sees scary prompt)
   - Apple App Store review scrutinises this permission heavily
   - Battery drain is significant
3. **Background fetch** — periodic wake-ups (~15–30 min, unreliable timing)

**Recommendation:** Use foreground-only tracking for MVP. When driver navigates away, show a persistent notification reminding them to return to the app. Implement background tracking in a future phase with the `@transistorsoft/capacitor-background-geolocation` premium plugin.

### React Native Migration Path

If deeper native integration is needed beyond Capacitor's capabilities:

1. **Shared API layer** — `src/services/` and `src/native/` abstractions map cleanly to React Native modules
2. **Shared state** — Zustand stores work in React Native unchanged
3. **UI rebuild required** — React Native needs `<View>` / `<Text>` instead of HTML/CSS
4. **Estimated effort** — 4–6 weeks to rebuild UI in RN while reusing business logic
5. **When to migrate** — If background tracking, Bluetooth (BLE beacons), or complex animations become requirements

---

## Feature Flags

| Flag | Default | Purpose |
|------|---------|---------|
| `VITE_CAPACITOR` | `false` | Disables PWA service worker for native builds |
| `isNative()` | runtime | Gates all Capacitor plugin calls |
| `VITE_NATIVE_API_URL` | unset | Absolute API URL for native WebView |
| `VITE_NATIVE_WS_URL` | unset | Absolute WebSocket URL for native |

---

## Testing Checklist

### Web (no regression)
- [ ] `npm run build` succeeds with PWA output (sw.js, manifest)
- [ ] Login works on localhost:4173 via preview proxy
- [ ] PWA install prompt appears on Chrome
- [ ] Offline banner shows when network disabled
- [ ] Service worker caches API responses

### Android Wrapper
- [ ] `npm run cap:build:android` completes without errors
- [ ] 12 Capacitor plugins found during sync
- [ ] Android Studio opens the project
- [ ] App launches in emulator/device
- [ ] Login works (with absolute API URL configured)
- [ ] Geolocation permission prompt appears
- [ ] Camera opens for proof-of-delivery
- [ ] Haptic feedback on order actions
- [ ] Status bar is orange (#f97316)
- [ ] Back button navigates / exits correctly

### Plugin Stubs (compile check)
- [ ] `@capacitor/geolocation` — watchPosition / getCurrentPosition
- [ ] `@capacitor/camera` — getPhoto with DataUrl result
- [ ] `@capacitor/push-notifications` — register, addListener
- [ ] `@capacitor/network` — getStatus, addListener
- [ ] `@capacitor/haptics` — impact, notification
- [ ] `@capacitor/app` — appStateChange, backButton
- [ ] `@capacitor/status-bar` — setStyle, setBackgroundColor
- [ ] `@capacitor/splash-screen` — hide
- [ ] `@capacitor/keyboard` — keyboardWillShow/Hide listeners

---

## Files Changed Summary

```
 18 files changed, ~1200 insertions

 New files:
   frontend/capacitor.config.ts
   frontend/src/native/platform.ts
   frontend/src/native/geolocation.ts
   frontend/src/native/camera.ts
   frontend/src/native/push.ts
   frontend/src/native/network.ts
   frontend/src/native/haptics.ts
   frontend/src/native/app-lifecycle.ts
   frontend/src/native/index.ts
   frontend/.env.capacitor.example
   frontend/android/                    (native Android project)
   docs/mobile-wrapper.md              (this file)

 Modified files:
   frontend/vite.config.ts             (conditional PWA)
   frontend/src/main.tsx               (native init)
   frontend/src/services/api.ts        (platform-aware URL)
   frontend/src/hooks/usePWA.ts        (isNativeApp flag)
   frontend/package.json               (deps + scripts)
   frontend/.env                       (VITE_CAPACITOR flag)
   .gitignore                          (Capacitor artifacts)
```
