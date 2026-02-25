# QuickBite — UI / Backend-Frontend Gap Analysis

> **Generated:** Phase 0 Automated Scan  
> **Backend:** 29 Controllers · 100+ REST Endpoints · 6 WebSocket Topics · 7 Scheduled Jobs  
> **Frontend:** 24 Routes · 24 Pages · 24 Components · 21 Services (83 API calls) · 4 Hooks · 3 Stores

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Backend Endpoints with NO Frontend Consumer](#2-backend-endpoints-with-no-frontend-consumer)
3. [Frontend Service Methods Never Called by Any Page](#3-frontend-service-methods-never-called-by-any-page)
4. [Missing Frontend Pages / Features](#4-missing-frontend-pages--features)
5. [Incomplete Frontend Implementations](#5-incomplete-frontend-implementations)
6. [Accessibility Audit](#6-accessibility-audit)
7. [Real-Time / WebSocket Gaps](#7-real-time--websocket-gaps)
8. [UI/UX Enhancement Opportunities](#8-uiux-enhancement-opportunities)
9. [Full Endpoint Coverage Matrix](#9-full-endpoint-coverage-matrix)

---

## 1. Executive Summary

| Metric | Count |
|--------|-------|
| Total backend REST endpoints | 107 |
| Endpoints consumed by frontend | 83 |
| Endpoints with **no frontend consumer** | 24 |
| Endpoints needing no frontend (webhooks, system) | 2 |
| **Actionable gaps** | **22** |
| Frontend service methods never called from pages | 4 |
| Missing admin pages (entire feature areas) | 4 |
| Incomplete page implementations | 5 |
| Accessibility issues found | 8 |
| WebSocket opportunities missed | 2 |

**Overall coverage: 78%** — The frontend covers the primary customer/vendor/driver flows well, but several admin capabilities and ancillary vendor features exist only in the backend with no UI.

---

## 2. Backend Endpoints with NO Frontend Consumer

### 2.1 Admin — Promo Code Management (5 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/promos` | Create promo code |
| PUT | `/api/promos/{id}` | Update promo code |
| GET | `/api/promos` | List all promo codes |
| GET | `/api/promos/{id}` | Get promo details |
| DELETE | `/api/promos/{id}` | Delete promo code |

**Impact:** ADMIN has no way to create/manage promo codes via UI. Only `GET /promos/validate` is used by customers at checkout.  
**Frontend file:** `promo.service.ts` has only `validatePromo()`.

### 2.2 Admin — Commission Management (2 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/commissions/{vendorId}` | Get vendor commission rate |
| PUT | `/api/admin/commissions/{vendorId}` | Set vendor commission rate |

**Impact:** Commission rates can only be set via direct API calls. No `admin.service.ts` method exists.

### 2.3 Admin — Review Moderation (2 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/admin/reviews/{reviewId}/hide` | Hide a vendor review |
| PUT | `/api/admin/driver-reviews/{reviewId}/hide` | Hide a driver review |

**Impact:** Flagged/offensive reviews cannot be moderated via UI.

### 2.4 Admin — Reports (2 endpoints — service exists, page doesn't call them)

| Method | Endpoint | Service Method | Called by Page? |
|--------|----------|---------------|----------------|
| GET | `/api/admin/reports/revenue` | `analyticsService.getRevenueReport()` | ❌ AdminReporting only uses `getPlatformKpis()` + `exportAdminCsv()` |
| GET | `/api/admin/reports/delivery-times` | `analyticsService.getDeliveryTimes()` | ❌ Not used anywhere |

**Impact:** Revenue breakdown and delivery-time analytics are invisible to admins despite working backend.

### 2.5 Admin — Feature Flag Details (1 endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/admin/feature-flags/details` | List flags with metadata (description, created date) |

**Impact:** AdminHealth uses `/feature-flags` (simple key/value map) but not `/details` (richer metadata). Minor gap.

### 2.6 Vendor — Scheduled Orders (1 endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/vendors/{vendorId}/scheduled-orders` | List upcoming scheduled orders for vendor |

**Impact:** Vendors have no way to see scheduled future orders. Orders with `scheduledTime` just appear in the normal order list.

### 2.7 Vendor — Reports (3 endpoints — entire controller unused)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/vendors/{vendorId}/reports/kpis` | Vendor-specific KPIs |
| GET | `/api/vendors/{vendorId}/reports/revenue` | Vendor revenue breakdown |
| GET | `/api/vendors/{vendorId}/reports/export` | Export vendor report CSV |

**Impact:** VendorDashboard Analytics tab uses `VendorAnalyticsController` (`/analytics`) but not `VendorReportController` (`/reports`). The reports controller provides different metrics (revenue time-series, custom KPIs) that are inaccessible.

### 2.8 Order — Schedule Validation (1 endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders/validate-schedule` | Validate a proposed scheduled order time |

**Impact:** Checkout allows setting `scheduledTime` but never pre-validates it. Invalid times will only fail at order creation.

### 2.9 Order — Manual Driver Assignment (1 endpoint)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/orders/{id}/assign/{driverId}` | Manually assign driver to order |

**Impact:** Admins cannot manually reassign drivers via UI. Only driver self-acceptance (`POST /drivers/orders/:id/accept`) is supported.

### 2.10 Modifier — Update Operations (2 endpoints)

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/modifier-groups/{groupId}` | Update modifier group |
| PUT | `/api/modifiers/{modifierId}` | Update a modifier |

**Impact:** Vendor can create/delete modifiers but cannot edit existing ones (name, price) without delete+recreate.

### 2.11 System Endpoints (no frontend needed)

| Method | Endpoint | Reason |
|--------|----------|--------|
| POST | `/api/payments/webhook` | Inbound from payment provider (HMAC-secured) |
| POST | `/api/payments/capture` | System/backend-only payment capture |

---

## 3. Frontend Service Methods Never Called by Any Page

| Service | Method | Endpoint | Notes |
|---------|--------|----------|-------|
| `analytics.service.ts` | `getRevenueReport()` | `GET /admin/reports/revenue` | Exists in service but AdminReporting doesn't call it |
| `analytics.service.ts` | `getDeliveryTimes()` | `GET /admin/reports/delivery-times` | Exists in service but no page calls it |
| `driver.service.ts` | `getRecentLocations()` | `GET /drivers/location/recent` | Exists in service but DriverDashboard doesn't display location history |
| `driver.service.ts` | `updateDriverStatus()` | `PUT /drivers/status` | Exists in service but DriverDashboard uses `startShift()` / `endShift()` instead |

---

## 4. Missing Frontend Pages / Features

### 4.1 Admin Promo Management Page — **Priority: HIGH**

**Why:** Promos are a core business feature. Backend has full CRUD but admins have zero UI to manage codes.

**Required:**
- Route: `/admin/promos`
- Page: `AdminPromos.tsx`
- Service additions: `promo.service.ts` — add `createPromo()`, `updatePromo()`, `getPromos()`, `getPromo()`, `deletePromo()`
- UI: Table with all promos (code, type, discount, usage stats, active status), create/edit modal, delete confirmation

**Backend endpoints to consume:**
- `GET /api/promos` — list all
- `GET /api/promos/{id}` — get details
- `POST /api/promos` — create
- `PUT /api/promos/{id}` — update
- `DELETE /api/promos/{id}` — delete

### 4.2 Admin Commission Management Page — **Priority: HIGH**

**Why:** Commission rates are a revenue-critical setting. Currently no UI exists.

**Required:**
- Route: `/admin/commissions` or integrate into AdminManagement page as a tab
- Service additions: `admin.service.ts` — add `getCommission(vendorId)`, `setCommission(vendorId, data)`
- UI: Vendor list with current commission rates, inline edit or modal for changing rates

**Backend endpoints to consume:**
- `GET /api/admin/commissions/{vendorId}`
- `PUT /api/admin/commissions/{vendorId}`

### 4.3 Admin Review Moderation Page — **Priority: MEDIUM**

**Why:** Content moderation is required for any public review system.

**Required:**
- Route: `/admin/reviews` or tab in AdminManagement
- Service additions: `admin.service.ts` — add `hideVendorReview(reviewId)`, `hideDriverReview(reviewId)`
- UI: Tabbed view (vendor reviews / driver reviews), report queue, hide/unhide actions

**Backend endpoints to consume:**
- `PUT /api/admin/reviews/{reviewId}/hide`
- `PUT /api/admin/driver-reviews/{reviewId}/hide`
- `GET /api/vendors/{vendorId}/reviews` (to browse)
- `GET /api/drivers/{driverId}/reviews` (to browse)

### 4.4 Vendor Scheduled Orders Tab — **Priority: MEDIUM**

**Why:** Vendors need advance preparation for scheduled orders.

**Required:**
- Tab in VendorDashboard: "Scheduled" with upcoming scheduled orders
- Service additions: `vendor.service.ts` — add `getScheduledOrders(vendorId)`
- UI: Timeline/list sorted by schedule time, countdown indicators

**Backend endpoints to consume:**
- `GET /api/vendors/{vendorId}/scheduled-orders`

---

## 5. Incomplete Frontend Implementations

### 5.1 AdminReporting Page — Only 2 of 4 Available Endpoints Used

| Status | Endpoint | Description |
|--------|----------|-------------|
| ✅ Used | `GET /admin/reports/kpis` | Platform KPIs |
| ✅ Used | `GET /admin/reports/export` | CSV export |
| ❌ Not used | `GET /admin/reports/revenue` | Revenue breakdown by day |
| ❌ Not used | `GET /admin/reports/delivery-times` | Delivery time statistics |

**Fix:** Add revenue chart (bar/line chart by day) and delivery-time metrics (avg, min, max, percentiles) sections to AdminReporting.

### 5.2 Checkout — No Schedule Validation

The Checkout page allows customers to pick a `scheduledTime` but **does not validate** it against vendor availability via `POST /orders/validate-schedule`.

**Fix:** Call `validateSchedule()` before order submission. Show error if vendor is closed or time is in the past.

### 5.3 VendorMenuManagement — No Modifier Editing

Vendors can create and delete modifiers/modifier groups but **cannot update** existing ones.

**Fix:** Add edit buttons to modifier groups and individual modifiers. Wire to `PUT /modifier-groups/{groupId}` and `PUT /modifiers/{modifierId}`.

### 5.4 ChatWindow — REST Polling Instead of WebSocket

ChatWindow polls via REST every 5 seconds. Backend already publishes to `/topic/chat.{roomId}` via STOMP.

**Fix:** Use `useOrderUpdates`-style STOMP subscription for `/topic/chat.{roomId}` with REST polling fallback.

### 5.5 VendorDashboard — Missing Vendor Reports

VendorDashboard Analytics tab uses `VendorAnalyticsController` but ignores `VendorReportController` which provides:
- Revenue time-series breakdown
- Vendor-specific KPIs (different from analytics)
- CSV report export

**Fix:** Add a "Reports" sub-tab or integrate vendor-report data alongside analytics.

---

## 6. Accessibility Audit

### 6.1 Issues Found

| # | Component / Page | Issue | Severity |
|---|-----------------|-------|----------|
| 1 | VendorList search input | Missing `<label>` or `aria-label` | HIGH |
| 2 | ChatWindow message input | Missing `<label>` or `aria-label` | HIGH |
| 3 | Form error messages | Use toast-only errors, no `aria-describedby` for field-level validation | MEDIUM |
| 4 | MenuItemCard "Add to Cart" | No `aria-label` with item name context | MEDIUM |
| 5 | StarRating interactive mode | No `aria-valuemin/max/now` for slider-like behavior | MEDIUM |
| 6 | Admin tables | No `scope` attributes on `<th>` elements | LOW |
| 7 | Modal dialogs | Need `aria-modal`, focus trap, and `Escape` key handling audit | MEDIUM |
| 8 | Color contrast | Orange theme (`#f97316`) on white may fail WCAG AA for small text | LOW |

### 6.2 What's Already Good

| Component | ARIA Feature |
|-----------|-------------|
| Header | `role="banner"`, `aria-label` on navs, `aria-expanded` on mobile toggle |
| ProtectedRoute | Proper redirect flow |
| LoadingSpinner | `role="status"`, `aria-label="Loading"` |
| Toast | `role="alert"`, `aria-live="assertive"` |

---

## 7. Real-Time / WebSocket Gaps

| Feature | Current | Optimal | Gap |
|---------|---------|---------|-----|
| Order updates | ✅ STOMP via `useOrderUpdates` hook | ✅ Done | None |
| Driver location | ✅ `useDriverLocation` broadcasts to `/topic/drivers.{id}.location` | ✅ Done | None |
| Vendor KDS | ❌ 10-sec REST polling in VendorDashboard | STOMP `/topic/vendors.{vendorId}.orders` | Should subscribe via WebSocket |
| Chat messages | ❌ 5-sec REST polling in ChatWindow | STOMP `/topic/chat.{roomId}` | Should subscribe via WebSocket |
| Notifications | ❌ 30-sec polling in NotificationBell | Could use STOMP or SSE | Nice-to-have |
| Driver order assignments | ❌ REST polling in DriverDashboard | STOMP `/topic/drivers.{driverId}` | Should subscribe via WebSocket |

---

## 8. UI/UX Enhancement Opportunities

### 8.1 Missing UI Patterns

| Pattern | Status | Recommendation |
|---------|--------|---------------|
| **Skeleton loading** | ❌ Only spinner used | Add skeleton screens for lists, cards, dashboards |
| **Empty states** | ❌ Most lists show nothing or plain text | Add illustrated empty states with CTAs |
| **Infinite scroll / pagination controls** | Partial (some pages have page/size params) | Add proper pagination UI or infinite scroll |
| **Dark mode** | ❌ Not implemented | Add Tailwind dark mode toggle |
| **Breadcrumbs** | ❌ No breadcrumb navigation | Add breadcrumbs for deep routes (order detail, admin sub-pages) |
| **Confirmation dialogs** | ❌ Uses `window.confirm()` | Replace with styled confirmation modals |
| **Drag-and-drop** | ❌ Menu item ordering is manual | Add drag-to-reorder for menu items |
| **Advanced filters** | ❌ VendorList has text search only | Add cuisine type, price range, rating filters |
| **Image upload preview** | Partial (proof photo has preview) | Add to menu item creation/vendor profile |
| **Keyboard navigation** | Partial | Add keyboard shortcuts for common actions |

### 8.2 Component-Specific Improvements

| Component | Current State | Enhancement |
|-----------|--------------|-------------|
| **Header** | Functional but basic | Active route highlighting, search bar in header |
| **VendorList** | Card grid, text search | Category filters, sort options (rating, distance, price), map view toggle |
| **VendorDetail** | Menu list with modifiers | Tabbed view (Menu / Reviews / Info), menu category sections, image gallery |
| **Cart** | Basic list | Quantity stepper, subtotal per item, saving estimates |
| **Checkout** | Functional flow | Order summary sidebar, better address picker (map integration), tip selection |
| **OrderTrack** | Status + history | Visual progress stepper, live map with driver location, ETA display |
| **MyOrders** | Flat list | Tabbed (Active / Past / Scheduled), status badges with colors |
| **VendorDashboard** | 6 tabs, functional | Sound alerts for new orders, drag-to-reorder KDS, daily summary |
| **DriverDashboard** | Comprehensive | Earnings summary widget, route optimization hint, delivery stats |
| **AdminHealth** | Metrics + flags | Charts/graphs for time-series, alert thresholds, log viewer |
| **AdminManagement** | User/vendor tables | Bulk actions, inline status toggles, activity log |
| **AdminReporting** | KPI cards + CSV | Charts (bar, line, pie), date range picker, comparison periods |
| **Profile** | Basic form | Avatar upload, change password form, 2FA setup |
| **Notifications** | Paginated list | Group by date, notification type icons, swipe-to-dismiss (mobile) |
| **Footer** | 4-column layout | Link to order tracking for guests, social media links |

### 8.3 Performance Improvements

| Area | Current | Optimization |
|------|---------|-------------|
| **Bundle size** | Manual chunks configured (vendor-react, vendor-data, vendor-stripe) | Audit chunk sizes, add dynamic imports for admin pages |
| **Image loading** | No lazy loading for menu item images | Add `loading="lazy"` on images, LQIP/blur-up placeholders |
| **List rendering** | Standard map rendering | Virtual scrolling for large order lists, menu lists |
| **API calls** | Some pages make multiple sequential calls | SWR / React Query for caching and deduplication |
| **Fonts** | Google Fonts with StaleWhileRevalidate cache | Preload critical fonts, font-display: swap |

---

## 9. Full Endpoint Coverage Matrix

### Legend
- ✅ = Frontend service + page consume this endpoint
- 🔶 = Frontend service exists but page doesn't call it
- ❌ = No frontend consumer at all
- ➖ = No frontend needed (webhook/system)

### Auth (9 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /auth/register` | ✅ |
| `POST /auth/login` | ✅ |
| `POST /auth/refresh` | ✅ |
| `POST /auth/logout` | ✅ |
| `GET /auth/me` | ✅ |
| `POST /auth/forgot-password` | ✅ |
| `POST /auth/reset-password` | ✅ |
| `POST /auth/verify-email` | ✅ |
| `POST /auth/resend-verification` | ✅ |

### Orders (9 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /orders` | ✅ |
| `GET /orders/{id}` | ✅ |
| `GET /orders` | ✅ |
| `PATCH /orders/{id}/status` | ✅ |
| `POST /orders/{id}/accept` | ✅ |
| `POST /orders/{id}/reject` | ✅ |
| `GET /orders/{id}/status-history` | ✅ |
| `POST /orders/{id}/assign/{driverId}` | ❌ |
| `POST /orders/{id}/reorder` | ✅ |

### Admin Orders (1 endpoint)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/orders/{id}/timeline` | ✅ |

### Scheduled Orders (2 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors/{vendorId}/scheduled-orders` | ❌ |
| `POST /orders/validate-schedule` | ❌ |

### Drivers (11 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /drivers/available-orders` | ✅ |
| `POST /drivers/orders/{orderId}/accept` | ✅ |
| `GET /drivers/active-delivery` | ✅ |
| `PUT /drivers/location` | ✅ |
| `GET /drivers/location/recent` | 🔶 |
| `POST /drivers/shift/start` | ✅ |
| `POST /drivers/shift/end` | ✅ |
| `GET /drivers/delivery-history` | ✅ |
| `GET /drivers/profile` | ✅ |
| `PUT /drivers/profile` | ✅ |
| `PUT /drivers/status` | 🔶 |

### Vendors (6 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors` | ✅ |
| `GET /vendors/{id}` | ✅ |
| `GET /vendors/search` | ✅ |
| `GET /vendors/my` | ✅ |
| `POST /vendors` | ✅ |
| `PUT /vendors/my` | ✅ |

### Menu Items (5 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors/{vendorId}/menu` | ✅ |
| `GET /menu-items/{id}` | ✅ |
| `POST /vendors/{vendorId}/menu` | ✅ |
| `PUT /menu-items/{id}` | ✅ |
| `DELETE /menu-items/{id}` | ✅ |

### Modifiers (7 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /menu-items/{itemId}/modifiers` | ✅ |
| `POST /menu-items/{itemId}/modifier-groups` | ✅ |
| `PUT /modifier-groups/{groupId}` | ❌ |
| `DELETE /modifier-groups/{groupId}` | ✅ |
| `POST /modifier-groups/{groupId}/modifiers` | ✅ |
| `PUT /modifiers/{modifierId}` | ❌ |
| `DELETE /modifiers/{modifierId}` | ✅ |

### Inventory (3 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors/{vendorId}/inventory` | ✅ |
| `PUT /vendors/{vendorId}/inventory/{itemId}` | ✅ |
| `POST /vendors/{vendorId}/inventory/reset-daily` | ✅ |

### Admin Commission (2 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/commissions/{vendorId}` | ❌ |
| `PUT /admin/commissions/{vendorId}` | ❌ |

### Users (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /users/me` | ✅ |
| `PUT /users/me` | ✅ |
| `GET /users/me/export` | ✅ |
| `DELETE /users/me` | ✅ |

### Addresses (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /addresses` | ✅ |
| `POST /addresses` | ✅ |
| `PUT /addresses/{id}` | ✅ |
| `DELETE /addresses/{id}` | ✅ |

### Payments (5 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /payments/intent` | ✅ |
| `GET /payments/{id}` | ✅ |
| `POST /payments/capture` | ➖ |
| `POST /payments/refund` | ✅ |
| `POST /payments/webhook` | ➖ |

### Promo Codes (6 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /promos/validate` | ✅ |
| `POST /promos` | ❌ |
| `PUT /promos/{id}` | ❌ |
| `GET /promos` | ❌ |
| `GET /promos/{id}` | ❌ |
| `DELETE /promos/{id}` | ❌ |

### Notifications (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /notifications` | ✅ |
| `GET /notifications/unread-count` | ✅ |
| `PATCH /notifications/{id}/read` | ✅ |
| `POST /notifications/read-all` | ✅ |

### Notification Preferences (2 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /notifications/preferences` | ✅ |
| `PUT /notifications/preferences` | ✅ |

### Reviews (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /orders/{orderId}/review` | ✅ |
| `GET /vendors/{vendorId}/reviews` | ✅ |
| `GET /vendors/{vendorId}/rating-summary` | ✅ |
| `PUT /admin/reviews/{reviewId}/hide` | ❌ |

### Driver Reviews (5 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /orders/{orderId}/driver-review` | ✅ |
| `GET /drivers/{driverId}/reviews` | ✅ |
| `GET /drivers/{driverId}/rating-summary` | ✅ |
| `PUT /driver-reviews/{reviewId}/dispute` | ✅ |
| `PUT /admin/driver-reviews/{reviewId}/hide` | ❌ |

### Chat (5 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /chat/rooms` | ✅ |
| `GET /chat/rooms` | ✅ |
| `GET /chat/rooms/{roomId}/messages` | ✅ |
| `POST /chat/rooms/{roomId}/messages` | ✅ |
| `PUT /chat/rooms/{roomId}/read` | ✅ |

### Devices (2 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /devices/register` | ✅ |
| `DELETE /devices/{token}` | ✅ |

### Favorites (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /favorites/{vendorId}` | ✅ |
| `DELETE /favorites/{vendorId}` | ✅ |
| `GET /favorites` | ✅ |
| `GET /favorites/{vendorId}/check` | ✅ |

### Delivery Proof (5 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `POST /orders/{orderId}/proof/photo` | ✅ |
| `POST /orders/{orderId}/proof/otp/generate` | ✅ |
| `POST /orders/{orderId}/proof/otp/verify` | ✅ |
| `GET /orders/{orderId}/proof` | ✅ |
| `GET /orders/{orderId}/proof/required` | ✅ |

### Health (1 endpoint)
| Endpoint | Coverage |
|----------|----------|
| `GET /health` | ✅ (implicit) |

### Admin Health (1 endpoint)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/health-summary` | ✅ |

### Feature Flags (3 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/feature-flags` | ✅ |
| `GET /admin/feature-flags/details` | ❌ |
| `PUT /admin/feature-flags/{key}` | ✅ |

### Admin Management (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/users` | ✅ |
| `PUT /admin/users/{userId}/status` | ✅ |
| `GET /admin/vendors` | ✅ |
| `PUT /admin/vendors/{vendorId}/approve` | ✅ |

### Admin Reports (4 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /admin/reports/kpis` | ✅ |
| `GET /admin/reports/revenue` | 🔶 |
| `GET /admin/reports/delivery-times` | 🔶 |
| `GET /admin/reports/export` | ✅ |

### Vendor Reports (3 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors/{vendorId}/reports/kpis` | ❌ |
| `GET /vendors/{vendorId}/reports/revenue` | ❌ |
| `GET /vendors/{vendorId}/reports/export` | ❌ |

### Vendor Analytics (2 endpoints)
| Endpoint | Coverage |
|----------|----------|
| `GET /vendors/{vendorId}/analytics` | ✅ |
| `GET /vendors/{vendorId}/analytics/export` | ✅ |

---

## Summary Statistics

| Category | ✅ Covered | 🔶 Partial | ❌ Missing | ➖ N/A |
|----------|-----------|-----------|-----------|--------|
| Auth | 9 | 0 | 0 | 0 |
| Orders | 8 | 0 | 1 | 0 |
| Scheduled Orders | 0 | 0 | 2 | 0 |
| Drivers | 9 | 2 | 0 | 0 |
| Vendors | 6 | 0 | 0 | 0 |
| Menu Items | 5 | 0 | 0 | 0 |
| Modifiers | 5 | 0 | 2 | 0 |
| Inventory | 3 | 0 | 0 | 0 |
| Commissions | 0 | 0 | 2 | 0 |
| Users | 4 | 0 | 0 | 0 |
| Addresses | 4 | 0 | 0 | 0 |
| Payments | 3 | 0 | 0 | 2 |
| Promos | 1 | 0 | 5 | 0 |
| Notifications | 4 | 0 | 0 | 0 |
| Notification Prefs | 2 | 0 | 0 | 0 |
| Reviews | 3 | 0 | 1 | 0 |
| Driver Reviews | 4 | 0 | 1 | 0 |
| Chat | 5 | 0 | 0 | 0 |
| Devices | 2 | 0 | 0 | 0 |
| Favorites | 4 | 0 | 0 | 0 |
| Delivery Proof | 5 | 0 | 0 | 0 |
| Health | 1 | 0 | 0 | 0 |
| Admin Health | 1 | 0 | 0 | 0 |
| Feature Flags | 2 | 0 | 1 | 0 |
| Admin Mgmt | 4 | 0 | 0 | 0 |
| Admin Reports | 2 | 2 | 0 | 0 |
| Vendor Reports | 0 | 0 | 3 | 0 |
| Vendor Analytics | 2 | 0 | 0 | 0 |
| **TOTAL** | **83** | **4** | **18** | **2** |
