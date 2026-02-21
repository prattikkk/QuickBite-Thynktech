# QuickBite → Deonde-Style Platform: Integration Gap Analysis (Day 0)

**Generated:** 2025-02-21  
**Branch:** `phase0/gap-analysis`  
**Baseline commit:** `bfad120` on `main`  
**Status:** Baseline audit before Phase 1 begins

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current Architecture Snapshot](#2-current-architecture-snapshot)
3. [Backend Endpoints Inventory](#3-backend-endpoints-inventory)
4. [Frontend Routes & Services Inventory](#4-frontend-routes--services-inventory)
5. [Endpoint-to-Frontend Wiring Matrix](#5-endpoint-to-frontend-wiring-matrix)
6. [Database Schema & Migration State](#6-database-schema--migration-state)
7. [WebSocket & Real-Time State](#7-websocket--real-time-state)
8. [Security & Auth Gaps](#8-security--auth-gaps)
9. [Testing Gaps](#9-testing-gaps)
10. [Feature Gap Matrix (Deonde Parity)](#10-feature-gap-matrix-deonde-parity)
11. [Missing Entities & Migrations Needed](#11-missing-entities--migrations-needed)
12. [Missing Endpoints Needed](#12-missing-endpoints-needed)
13. [Missing Frontend Pages & Components](#13-missing-frontend-pages--components)
14. [Phase Execution Roadmap](#14-phase-execution-roadmap)

---

## 1. Executive Summary

QuickBite is a working **single-tenant food delivery monolith** with:

- **16 controllers, ~55 endpoints** (Spring Boot 3.2.2 / Java 17)
- **19 entity classes**, 16 Flyway migrations (PostgreSQL 13)
- **17 frontend pages**, 12 service modules (React 18 + TypeScript + Vite)
- **~195 passing tests** (162 backend @Test methods + Cypress E2E suite)
- JWT auth with refresh token rotation, role-based access (CUSTOMER, VENDOR, DRIVER, ADMIN)
- Full order lifecycle: PLACED → ACCEPTED → PREPARING → READY → ASSIGNED → PICKED_UP → ENROUTE → DELIVERED
- Payment intents, webhook processing with DLQ + retry, idempotency keys
- Promotions engine, favorites, notifications, ETA estimation
- WebSocket (STOMP/SockJS) for order status updates
- Prometheus metrics, structured logging, feature flags, admin health dashboard

### What's Working Well

| Area | Status |
|------|--------|
| Order lifecycle & state machine | ✅ Complete with guards |
| Payment flow (Stripe sandbox) | ✅ Intent → webhook → capture |
| Webhook reliability (retry + DLQ) | ✅ Implemented |
| Idempotency on write APIs | ✅ Implemented |
| JWT + refresh token rotation | ✅ Implemented |
| Admin user/vendor management | ✅ Implemented |
| Promo engine | ✅ Implemented |
| Favorites & reorder | ✅ Implemented |
| Notifications (in-app) | ✅ Implemented |
| Feature flags | ✅ Implemented |
| Metrics & health dashboard | ✅ Implemented |

### Critical Gaps for Deonde-Parity

| # | Gap | Severity | Phase |
|---|-----|----------|-------|
| G1 | No real-time driver location tracking (map) | **Critical** | Phase 1–2 |
| G2 | No customer-facing delivery map | **Critical** | Phase 2 |
| G3 | No PWA support (no service worker, no manifest) | **Critical** | Phase 4 |
| G4 | No push notifications (FCM/APN) | **High** | Phase 3 |
| G5 | No reviews/ratings system | **Medium** | Future |
| G6 | No multi-tenancy (white-label SaaS) | **Medium** | Enterprise |
| G7 | No vendor analytics/settlements/commission | **Medium** | Future |
| G8 | No driver profile (vehicle, online/offline) | **High** | Phase 1 |
| G9 | Refresh token not in HttpOnly cookie | **Medium** | Phase 1 |
| G10 | No vendor KDS view | **High** | Phase 1 |
| G11 | 2 orphaned page components (VendorMenuManagement, VendorProfile) | **Low** | Phase 1 |
| G12 | No OTP/photo proof-of-delivery | **High** | Phase 3 |
| G13 | No Capacitor/mobile wrapper | **Low** | Phase 5 |
| G14 | AuditLog entity exists but nothing writes to it | **Medium** | Phase 1 |
| G15 | WebSocket topics limited to order status only | **High** | Phase 1–2 |

---

## 2. Current Architecture Snapshot

```
┌──────────────────────────────────────────────────────┐
│                  Frontend (React 18 / Vite)           │
│  17 pages · 12 services · 10 components · Zustand    │
│  WebSocket (STOMP/SockJS) · Stripe Elements          │
│  Port 5173 (dev)                                     │
└──────────────┬───────────────────────────────────────┘
               │ /api/* (Axios) + /ws (STOMP)
┌──────────────▼───────────────────────────────────────┐
│            Backend (Spring Boot 3.2.2)                │
│  16 controllers · ~55 endpoints · JWT auth            │
│  19 entities · 16 Flyway migrations                   │
│  WebSocket (STOMP broker on /topic)                   │
│  Port 8080                                            │
├────────────────┬──────────────────┬──────────────────┤
│  PostgreSQL 13 │     Redis 6      │  (No RabbitMQ)   │
│  19 tables     │  Rate limiting   │                  │
│  Port 5432     │  Port 6379       │                  │
└────────────────┴──────────────────┴──────────────────┘
```

**Tech Stack:**

- **Backend:** Spring Boot 3.2.2, Java 17, Spring Security, Spring WebSocket, Flyway
- **Frontend:** React 18.2, TypeScript 5.3, Vite 5.1, Tailwind CSS 3.4, Zustand 4.5
- **Database:** PostgreSQL 13 + Flyway (V1–V16)
- **Cache:** Redis 6 (rate limiting)
- **Payments:** Stripe (sandbox)
- **Testing:** JUnit 5, Mockito, Testcontainers, Cypress 13.6
- **Observability:** Micrometer + Prometheus, structured logging, correlation IDs
- **Docker:** docker-compose.yml (dev) + docker-compose.prod.yml + k8s/deploy.yaml

---

## 3. Backend Endpoints Inventory

**16 controllers, ~55 endpoints total:**

| Controller | Base Path | # Endpoints | Auth |
|---|---|---|---|
| AuthenticationController | `/api/auth` | 5 (register, login, refresh, logout, me) | Public (except /me) |
| OrderController | `/api/orders` | 9 (CRUD, status, accept, reject, history, assign, reorder) | Role-based |
| DriverController | `/api/drivers` | 5 (available, accept, active, location, history) | DRIVER |
| VendorController | `/api/vendors` | 6 (list, getById, search, my, create, update) | Role-based |
| MenuItemController | `/api/vendors/*/menu` + `/api/menu-items` | 5 (list, getById, create, update, delete) | Role-based |
| PaymentController | `/api/payments` | 5 (intent, getById, capture, refund, webhook) | Role-based + webhook |
| NotificationController | `/api/notifications` | 4 (list, unread-count, read, read-all) | Authenticated |
| UserAccountController | `/api/users` | 4 (me, update, export, delete) | Authenticated |
| AddressController | `/api/addresses` | 4 (list, create, update, delete) | Authenticated |
| FavoriteController | `/api/favorites` | 4 (add, remove, list, check) | CUSTOMER |
| PromoCodeController | `/api/promos` | 6 (CRUD + validate) | CUSTOMER/ADMIN |
| AdminOrderController | `/api/admin/orders` | 1 (timeline) | ADMIN |
| AdminManagementController | `/api/admin` | 4 (list users, user status, list vendors, approve vendor) | ADMIN |
| AdminHealthController | `/api/admin/health-summary` | 1 | ADMIN |
| FeatureFlagController | `/api/admin/feature-flags` | 3 (list, get, toggle) | ADMIN |
| HealthController | `/api/health` | 1 | Public |

---

## 4. Frontend Routes & Services Inventory

### Routes (17 pages, 15 routed)

| Path | Component | Guard |
|---|---|---|
| `/login` | Login | Public |
| `/register` | Register | Public |
| `/vendors` | VendorList | CUSTOMER |
| `/vendors/:id` | VendorDetail | CUSTOMER |
| `/cart` | Cart | CUSTOMER |
| `/checkout` | Checkout | CUSTOMER |
| `/favorites` | Favorites | CUSTOMER |
| `/orders` | MyOrders | CUSTOMER |
| `/orders/:id` | OrderTrack | Any auth |
| `/profile` | Profile | Any auth |
| `/vendor/dashboard` | VendorDashboard | VENDOR |
| `/driver/dashboard` | DriverDashboard | DRIVER |
| `/admin/orders/:orderId/timeline` | AdminOrderTimeline | ADMIN |
| `/admin/health` | AdminHealth | ADMIN |
| `/admin/management` | AdminManagement | ADMIN |
| `/` | RoleBasedRedirect | — |
| `*` | NotFound → redirects to `/` | — |

### Services (12 modules)

`auth.service.ts`, `order.service.ts`, `vendor.service.ts`, `payment.service.ts`, `driver.service.ts`, `admin.service.ts`, `favorite.service.ts`, `notification.service.ts`, `promo.service.ts`, `user.service.ts`, `address.service.ts`, `api.ts` (base Axios)

### Zustand Stores (3)

`authStore` (user + tokens + role), `cartStore` (items + vendor lock), `notificationStore` (count + list)

### Orphaned Files (exist but not routed)

- **`VendorMenuManagement.tsx`** — full page with CRUD, not exported or routed
- **`VendorProfile.tsx`** — full page, not exported or routed

---

## 5. Endpoint-to-Frontend Wiring Matrix

### ✅ Fully Wired (both backend + frontend consumer exist)

| Backend Area | Frontend Consumer |
|---|---|
| Auth (register, login, refresh, logout, me) | `auth.service.ts` |
| Orders CRUD + status + accept/reject + reorder | `order.service.ts` |
| Vendors list/detail/search/my/create/update | `vendor.service.ts` |
| Menu items CRUD | `vendor.service.ts` |
| Payments intent/get | `payment.service.ts` |
| Driver available/accept/active/location/history | `driver.service.ts` |
| Favorites add/remove/list/check | `favorite.service.ts` |
| Notifications list/unread/read/readAll | `notification.service.ts` |
| Promos validate | `promo.service.ts` |
| User profile CRUD + export + delete | `user.service.ts` |
| Addresses CRUD | `address.service.ts` |
| Admin timeline/health/flags/users/vendors | `admin.service.ts` |

### ⚠️ Backend Endpoints Without Frontend Consumers

| # | Endpoint | Why Unwired |
|---|----------|-------------|
| 1 | `GET /api/menu-items/{id}` | Single-item view not needed in current UI |
| 2 | `POST /api/payments/capture` | Server-side webhook flow; no admin capture UI |
| 3 | `POST /api/payments/refund` | Server-side; no admin refund UI page |
| 4 | `POST /api/payments/webhook` | Provider callback; never called from frontend |
| 5 | `GET /api/admin/feature-flags/details` | Frontend uses flat map endpoint instead |
| 6 | `POST /api/orders/{id}/assign/{driverId}` | Admin/system operation — no dispatch UI |
| 7 | `POST /api/promos` (create) | No admin promo management page |
| 8 | `PUT /api/promos/{id}` (update) | No admin promo management page |
| 9 | `GET /api/promos` (list all) | No admin promo management page |
| 10 | `GET /api/promos/{id}` (get one) | No admin promo management page |
| 11 | `DELETE /api/promos/{id}` | No admin promo management page |

### ⚠️ Frontend DTO/Field Mismatches

**None found** — all frontend TypeScript type definitions currently align with backend response shapes. The `api.ts` interceptor extracts `.data.data` automatically.

---

## 6. Database Schema & Migration State

### Current Migrations (V1–V16)

| Migration | Creates/Alters |
|---|---|
| V1 | Initial roles + users (serial PK — superseded by V2) |
| V2 | Full UUID schema: roles, users, vendors, menu_items, addresses, orders, order_items, payments, delivery_status, token_store, audit_logs |
| V3 | Seed data (admin, customer, driver, 3 vendors with menus) |
| V4 | Order payment fields (order_number, subtotal_cents, etc.) |
| V5 | `webhook_events` table |
| V6 | Normalize statuses (COMPLETED→DELIVERED, CONFIRMED→ACCEPTED) |
| V7 | Payment `client_secret` column |
| V8 | `idempotency_keys` table |
| V9 | Webhook retry fields + `webhook_dlq` table |
| V10 | `event_timeline` audit table |
| V11 | `feature_flags` table + seed flags |
| V12 | `favorites` table |
| V13 | `promo_codes` table + order discount fields |
| V14 | `notifications` table |
| V15 | ETA fields on orders |
| V16 | Enable promos flag + seed promo codes |

### Current Entities (19)

`User`, `Role`, `Address`, `Vendor`, `MenuItem`, `Order`, `OrderItem`, `Payment`, `DeliveryStatus`, `TokenStore`, `WebhookEvent`, `WebhookDlq`, `EventTimeline`, `FeatureFlag`, `IdempotencyKey`, `AuditLog`, `Notification`, `Favorite`, `PromoCode`

### Missing Tables for Deonde Parity

| Table | Purpose | Priority |
|---|---|---|
| `driver_profiles` | Vehicle type, license, online/offline, location, stats | **High** (Phase 1) |
| `delivery_proofs` | OTP/photo proof-of-delivery | **High** (Phase 3) |
| `push_tokens` | FCM/APN device tokens | **High** (Phase 3) |
| `reviews` | Vendor + driver ratings/reviews | Medium |
| `zones` / `service_areas` | Geofence-based delivery zones | Medium |
| `delivery_fee_rules` | Dynamic fee calculation | Medium |
| `commissions` | Platform commission per vendor | Medium |
| `settlements` / `payouts` | Vendor/driver payout tracking | Medium |
| `tax_rules` | Tax by zone/category | Low |
| `wallets` / `wallet_transactions` | Customer/driver wallet | Low |
| `referrals` | Referral tracking | Low |
| `banners` | Marketing banners/CMS | Low |
| `support_tickets` | Customer support flow | Low |
| `categories` / `cuisines` | Normalized taxonomy | Low |
| `server_carts` / `cart_items` | Server-side cart | Low |
| `tenants` + tenant_id FKs | Multi-tenant SaaS | Enterprise |

---

## 7. WebSocket & Real-Time State

### Current Implementation

| Aspect | Status |
|---|---|
| STOMP broker | ✅ Simple broker on `/topic` |
| SockJS endpoint | ✅ `/ws` (browsers) + `/ws-native` (Node clients) |
| JWT auth on STOMP | ✅ Channel interceptor validates Bearer token |
| Topic `/topic/orders.{orderId}` | ✅ Order status updates (full order DTO payload) |
| Frontend hook `useOrderUpdates` | ✅ With automatic polling fallback |

### Missing Topics for Deonde Parity

| Topic | Purpose | Phase |
|---|---|---|
| `/topic/drivers.{driverId}` | New order assignment push to driver | Phase 1 |
| `/topic/drivers.{driverId}.location` | Driver GPS stream to customer map | Phase 2 |
| `/topic/vendors.{vendorId}.orders` | Live new-order feed for vendor KDS | Phase 1 |
| `/topic/users.{userId}.notifications` | Real-time push (replaces 30s polling) | Phase 3 |
| `/topic/admin.dashboard` | Live ops metrics for admin | Future |

### Frontend Notification Approach

- `NotificationBell` component polls `/api/notifications/unread-count` every **30 seconds**
- No WebSocket subscription for notifications
- No Web Push / FCM integration

---

## 8. Security & Auth Gaps

| # | Gap | Severity | Recommendation |
|---|-----|----------|----------------|
| S1 | **Refresh token in JSON body** (not HttpOnly cookie) | High | Move to HttpOnly Secure SameSite cookie |
| S2 | **WebSocket auth silently ignores invalid tokens** | Medium | Reject STOMP CONNECT on auth failure |
| S3 | **CORS wildcard on WebSocket** | Medium | Restrict to known origins |
| S4 | **AuditLog entity writes nothing** | Medium | Wire admin actions to AuditLog |
| S5 | **Stripe test keys in version-controlled properties** | Medium | Move to env vars only |
| S6 | **No OAuth2 / social login** | Low | Add Google/Facebook SSO |
| S7 | **No 2FA/MFA** | Low | Add TOTP for vendor/admin |
| S8 | **No API key auth for integrators** | Low | Future (enterprise) |
| S9 | **No CSP headers** | Low | Add Content-Security-Policy |
| S10 | **No per-endpoint rate limiting tiers** | Low | Enhance rate limiter |

---

## 9. Testing Gaps

### Current Test Coverage

| Layer | Count | Details |
|---|---|---|
| Service unit tests | ~95 | OrderService, Favorite, Promo, Notification, Idempotency, FeatureFlag, ETA, StateMachine, WebhookProcessor, EventTimeline, CorrelationFilter |
| Integration tests | ~67 | Auth, Order, Driver, Vendor, UserAccount, Admin, Notification, Promo, Favorite, Repos |
| E2E (Cypress) | ~28 | Customer flow, vendor flow, driver flow, webhook, auth, edge cases |
| **Total** | **~190** | |

### Missing Tests

| Gap | Priority |
|---|---|
| **No WebSocket E2E tests** (connect → subscribe → receive) | High (Phase 1) |
| **No load/performance tests in CI** (k6 scripts exist but JS-only) | Medium |
| **No contract/OpenAPI validation tests** | Medium |
| **Cypress tests are mostly API-driven** — minimal actual UI interaction | Medium |
| **FavoriteControllerIntegrationTest** has only 1 test | Low |
| **NotificationControllerIntegrationTest** has only 3 tests | Low |
| **PromoCodeControllerIntegrationTest** has only 3 tests | Low |
| **No security/penetration tests** | Low |
| **No chaos/resilience tests** | Low |

---

## 10. Feature Gap Matrix (Deonde Parity)

| Feature Area | QuickBite Status | Deonde Equivalent | Gap Level |
|---|---|---|---|
| **Multi-vendor marketplace** | ✅ Vendors, menus, pricing | ✅ On par | None |
| **Customer web app** | ✅ Browse, order, track, pay | No map tracking, no reviews, no scheduled orders | Medium |
| **Vendor portal** | ✅ Menu CRUD, order management | No KDS, no analytics, no commission view | Medium |
| **Driver app** | ✅ Accept, pickup, deliver, location | No map/nav, no vehicle profile, no earnings | **High** |
| **Order lifecycle** | ✅ Full state machine with guards | ✅ On par | None |
| **Payments** | ✅ Stripe + webhooks + DLQ | No settlements, no split pay | Medium |
| **Webhooks** | ✅ Idempotent + DLQ + retry | ✅ On par | None |
| **Real-time updates** | ⚠️ Order status only (1 topic) | No driver location, no vendor feed, no push | **High** |
| **Auth & RBAC** | ✅ JWT + refresh + 4 roles | Refresh not HttpOnly, no OAuth2, no MFA | Medium |
| **Admin dashboard** | ✅ Health, users, vendors, flags | No dispatch UI, no analytics, no promo mgmt | Medium |
| **Notifications** | ⚠️ In-app only (30s polling) | No push (FCM), no SMS, no email | **High** |
| **Promotions** | ✅ Engine + validation | No admin promo UI | Low |
| **Favorites & reorder** | ✅ Implemented | ✅ On par | None |
| **ETA** | ✅ Prep + travel estimate | No live map update | Low |
| **Feature flags** | ✅ Implemented | ✅ On par | None |
| **Observability** | ✅ Prometheus, logs, correlation | ✅ On par | None |
| **KDS** | ❌ Not built | Full kitchen display system | **High** |
| **PWA** | ❌ Not built | Install prompt, offline support | **High** |
| **Proof of delivery** | ❌ Not built | OTP/photo proof | **High** |
| **Reviews/ratings** | ❌ Not built | Vendor + driver reviews | Medium |
| **Driver dispatch scoring** | ⚠️ Basic (distance + fallback) | Scoring with distance, prep, load, success rate | Medium |
| **Multi-tenancy** | ❌ Not built | White-label SaaS | Enterprise |
| **Mobile native** | ❌ Not built | Capacitor / React Native | Phase 5 |

---

## 11. Missing Entities & Migrations Needed

### Phase 1 (Driver Dashboard Enhancement)

```sql
-- V17__add_driver_profiles.sql
CREATE TABLE driver_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id),
    vehicle_type VARCHAR(50),          -- BICYCLE, MOTORCYCLE, CAR
    license_plate VARCHAR(50),
    is_online BOOLEAN DEFAULT FALSE,
    current_lat DECIMAL(10,8),
    current_lng DECIMAL(11,8),
    last_seen_at TIMESTAMPTZ,
    total_deliveries INT DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 100.0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### Phase 3 (Proof-of-Delivery + Push)

```sql
-- V18__add_proofs_and_push_tokens.sql
CREATE TABLE delivery_proofs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id),
    proof_type VARCHAR(20) NOT NULL,    -- OTP, PHOTO, SIGNATURE
    otp_code VARCHAR(10),
    photo_url TEXT,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE push_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL,      -- WEB, ANDROID, IOS
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 12. Missing Endpoints Needed

### Phase 1

| Method | Path | Purpose |
|---|---|---|
| PUT | `/api/drivers/status` | Toggle online/offline |
| GET | `/api/drivers/profile` | Get driver profile (vehicle, stats) |
| PUT | `/api/drivers/profile` | Update driver profile |

### Phase 2

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/drivers/location` (enhanced) | Rate-limited GPS with WebSocket broadcast |
| GET | `/api/orders/{id}/driver-location` | Customer reads driver's last known position |

### Phase 3

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/orders/{id}/proof` | Submit delivery proof (OTP or photo) |
| POST | `/api/push-tokens` | Register FCM/Web Push token |
| DELETE | `/api/push-tokens/{token}` | Unregister push token |

### Future

| Method | Path | Purpose |
|---|---|---|
| POST/GET | `/api/reviews` | Submit/list reviews |
| GET | `/api/vendors/{id}/reviews` | Vendor reviews |
| GET/POST | `/api/admin/settlements` | Settlement management |
| GET/POST | `/api/admin/zones` | Zone management |
| GET | `/api/vendors/{id}/analytics` | Vendor analytics |
| GET | `/api/drivers/{id}/earnings` | Driver earnings |
| POST | `/api/admin/reports/generate` | Report generation |

---

## 13. Missing Frontend Pages & Components

### Phase 1

| Item | Purpose |
|---|---|
| `DriverDashboard.tsx` enhancement | Online/offline toggle, vehicle profile, earnings tab |
| `VendorKDS.tsx` | Kitchen display — large-tile auto-updating order board |
| Wire `VendorMenuManagement.tsx` | Already exists; needs route + export |
| Wire `VendorProfile.tsx` | Already exists; needs route + export |
| `ErrorBoundary.tsx` | React error boundary wrapper |

### Phase 2

| Item | Purpose |
|---|---|
| `MapComponent.tsx` | Reusable map (Leaflet/Google Maps) |
| `OrderTrack.tsx` enhancement | Map showing driver position + live ETA |
| `useDriverLocation` hook | Geolocation watch + POST loop for driver shift |

### Phase 3

| Item | Purpose |
|---|---|
| `ProofOfDelivery.tsx` | OTP entry + camera/photo upload in driver flow |
| `PushNotificationPrompt.tsx` | Web Push permission + FCM registration |

### Phase 4

| Item | Purpose |
|---|---|
| `vite-plugin-pwa` + `manifest.json` | PWA installability |
| Service worker (`sw.ts`) | Offline caching, push handler |
| `InstallPWAPrompt.tsx` | Install guidance component |

### Missing npm Dependencies

| Package | Purpose | Phase |
|---|---|---|
| `leaflet` + `react-leaflet` | Map rendering | Phase 2 |
| `vite-plugin-pwa` | PWA build plugin | Phase 4 |
| `firebase` or `web-push` | Push notifications | Phase 3 |
| `@capacitor/core` | Mobile wrapper | Phase 5 |

---

## 14. Phase Execution Roadmap

### Phase 0 — Gap Analysis ← **YOU ARE HERE**

- [x] Full backend scan (16 controllers, 19 entities, 16 migrations)
- [x] Full frontend scan (17 pages, 12 services, 3 stores)
- [x] Produce `docs/integration-gap-day0.md`
- [x] Branch `phase0/gap-analysis`
- [x] PR draft content

### Phase 1 — Driver Dashboard Enhancement (~2 sprints)

**Backend:**
- V17 migration: `driver_profiles` table
- `DriverProfile` entity + repository
- Driver online/offline status endpoint
- WebSocket topics: `/topic/drivers.{driverId}`, `/topic/vendors.{vendorId}.orders`
- Wire AuditLog for admin actions
- Enhanced driver scoring (distance + load + success rate)

**Frontend:**
- DriverDashboard: online/offline toggle, vehicle profile section, earnings tab
- VendorKDS page: kitchen display with auto-refresh
- Wire VendorMenuManagement + VendorProfile routes
- ErrorBoundary component
- WebSocket subscriptions for driver + vendor topics

**Tests:**
- Driver status transition unit + integration tests
- WebSocket connect/subscribe E2E tests
- KDS rendering tests

### Phase 2 — Foreground Live Location (~1 sprint)

**Backend:**
- Enhanced location endpoint with broadcast
- Customer-facing driver location query
- WebSocket topic `/topic/drivers.{driverId}.location`
- Rate limiting on GPS updates (max 1/3s)

**Frontend:**
- Install Leaflet/react-leaflet
- `useDriverLocation` hook with `watchPosition`
- Start/Stop Shift toggle + GPS indicator
- Map on OrderTrack (driver position + route)
- Map on DriverDashboard (current delivery)

### Phase 3 — Proof-of-Delivery & Push Notifications (~2 sprints)

**Backend:**
- V18 migration: `delivery_proofs` + `push_tokens`
- Proof submission endpoint (OTP + photo)
- FCM integration (pluggable NotificationSender)
- Web Push VAPID setup
- Notification via WebSocket topic

**Frontend:**
- ProofOfDelivery component (OTP + camera)
- Push permission prompt + FCM token registration
- Service worker push event handler

### Phase 4 — PWA Install Flow (~1 sprint)

- `vite-plugin-pwa` + manifest
- Service worker for offline caching
- Install prompt component
- Standalone mode verification

### Phase 5 — Mobile Wrapper Readiness (~1 sprint)

- Capacitor skeleton
- Plugin stubs (background geo, native push)
- Android build scripts
- Documentation

### Timeline Summary

| Phase | Duration | Dependencies |
|---|---|---|
| Phase 0 (this document) | 1 day | — |
| Phase 1 (driver dashboard) | 2 weeks | — |
| Phase 2 (live location) | 1 week | Phase 1 |
| Phase 3 (proof + push) | 2 weeks | Phase 1 |
| Phase 4 (PWA) | 1 week | Phase 2–3 |
| Phase 5 (mobile) | 1 week | Phase 4 |
| **MVP Total** | **~8 weeks** | |

---

*End of Day 0 Gap Analysis — awaiting confirmation to begin Phase 1.*
