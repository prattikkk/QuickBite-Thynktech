# QuickBite PWA — Driver Install Guide

## What is the QuickBite PWA?

QuickBite can be installed as an app directly from your phone's browser — **no app-store download required**. Once installed it looks and feels like a native app: full-screen, home-screen icon, fast startup, and offline caching for pages you've already visited.

---

## Requirements

| Requirement | Detail |
|-------------|--------|
| **Browser** | Chrome 89+, Edge 89+, Samsung Internet 15+, Opera 76+ |
| **OS** | Android 8+ (recommended), Windows 10+, macOS 12+ |
| **iOS** | Safari 16.4+ (partial PWA support — see notes below) |
| **Connection** | Internet access during first install; cached pages work offline |

---

## Install Steps (Android — Chrome)

1. **Open** `https://quickbite.example.com` (or the URL your admin provided) in **Chrome**.
2. **Log in** with your driver credentials.
3. You should see an orange **"Install QuickBite"** banner at the bottom of the screen.
   - Tap **Install**.
   - Chrome shows a native dialog — tap **"Add to Home screen"** → **Add**.
4. The QuickBite icon now appears on your home screen. Tap it to launch in standalone mode.

> **If the banner doesn't appear:**
> Tap the Chrome **⋮ menu** (top-right) → **"Install app"** or **"Add to Home screen"**.

---

## Install Steps (iOS — Safari)

> ⚠️ iOS PWA support has some limitations (no background push, no install-prompt banner). These steps add the app to your home screen.

1. Open `https://quickbite.example.com` in **Safari** (not Chrome for iOS).
2. Tap the **Share** button (box with an arrow, bottom center).
3. Scroll down and tap **"Add to Home Screen"**.
4. Tap **Add** in the top-right corner.
5. The QuickBite icon now appears on your home screen.

---

## Install Steps (Desktop — Chrome / Edge)

1. Navigate to `https://quickbite.example.com`.
2. Look for the **install icon** in the address bar (a circled plus or monitor with download arrow).
3. Click it → **Install**.
4. The app opens in its own window and appears in your taskbar / dock.

---

## Post-Install Checklist

| Check | How to verify |
|-------|---------------|
| App opens full-screen (no address bar) | Launch from home-screen icon |
| Login persists | You should remain logged in after closing and reopening |
| Live location works | Start Shift → grant location permission → backend shows location |
| Real-time order updates | Accept an order; status changes appear instantly |
| Notifications | In-app notification bell shows new orders and updates |
| Offline fallback | Turn on airplane mode → navigate → see "You're Offline" page |

---

## Updating the App

When a new version is deployed:

1. A **blue "New version available"** banner appears at the bottom.
2. Tap **Refresh** to apply the update immediately.
3. If you dismiss it, the update applies automatically next time you close and reopen the app.

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Install banner doesn't appear | Make sure you're using Chrome/Edge on Android. Check that the site is served over HTTPS. |
| App stuck on old version | Close the app completely, clear browser cache for the site, re-open. |
| Location not updating | Check device location permissions for the browser. Ensure GPS is on. |
| Can't receive push | Push notification support in PWA is limited on iOS. Use the in-app notification bell. |
| White screen after install | Force-close and reopen. If persists, uninstall and reinstall. |

---

## Uninstalling

- **Android**: Long-press the QuickBite icon → **Uninstall** or **Remove**.
- **iOS**: Long-press → tap the **X** / **Remove App**.
- **Desktop**: Open the app → click the **⋮ menu** (top-right) → **Uninstall QuickBite**.

---

## Technical Notes (for developers)

- PWA uses **vite-plugin-pwa** with Workbox `GenerateSW` strategy.
- Service worker registered via `virtual:pwa-register/react`.
- Runtime caching:
  - `NetworkFirst` for `/api/vendors`, `/api/orders`, `/api/drivers`, `/api/notifications` (5-min cache).
  - `CacheFirst` for `/uploads/proofs/` images (7-day cache).
  - Static assets (JS, CSS, HTML, fonts) are pre-cached on install.
- `navigateFallback: 'index.html'` ensures SPA routing works offline for cached pages.
- Deny-list: `/api/*`, `/ws/*`, `/uploads/*` are NOT intercepted for navigation fallback.
- SW update check interval: 60 minutes.
- `registerType: 'prompt'` — user is prompted to apply updates (not auto-reloaded).
