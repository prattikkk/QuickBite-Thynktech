# QuickBite — UI Enhancement Plan (PRD)

> **Based on:** `docs/ui-gap-analysis.md` Phase 0 Audit  
> **Scope:** Frontend-only changes unless backend API is absent  
> **Branch convention:** `ui/<priority>/<component>-enhancement`

---

## Table of Contents

1. [Priority Framework](#1-priority-framework)
2. [HIGH Priority — Must Have](#2-high-priority--must-have)
3. [MEDIUM Priority — Should Have](#3-medium-priority--should-have)
4. [LOW Priority — Nice to Have](#4-low-priority--nice-to-have)
5. [Implementation Phases](#5-implementation-phases)
6. [Design System Standards](#6-design-system-standards)
7. [Component Implementation Checklist](#7-component-implementation-checklist)
8. [Testing & Verification Plan](#8-testing--verification-plan)
9. [Release & Rollback Plan](#9-release--rollback-plan)

---

## 1. Priority Framework

| Priority | Criteria | Timeline |
|----------|----------|----------|
| **HIGH** | Missing admin features for core business ops, data loss risk, broken UX | Phase 1 (Days 1-3) |
| **MEDIUM** | Incomplete implementations, real-time upgrades, accessibility | Phase 2 (Days 4-6) |
| **LOW** | Polish, UX enhancements, advanced filters, dark mode | Phase 3 (Days 7-10) |

---

## 2. HIGH Priority — Must Have

### H1: Admin Promo Management Page

**Gap:** 5 backend CRUD endpoints for promo codes have zero frontend UI.  
**Business Impact:** Admins cannot create, edit, or manage promotional codes.  
**Branch:** `ui/high/admin-promos`

**Deliverables:**
- [ ] Route: `/admin/promos` (lazy loaded, ADMIN role guard)
- [ ] Page: `AdminPromos.tsx`
  - Table listing all promos with columns: Code, Type, Discount, Min Order, Max Uses, Used Count, Per-User Limit, Active, Expiry
  - Create Promo modal (form with all fields from `PromoCreateRequest`)
  - Edit Promo modal (pre-populated form)
  - Delete confirmation dialog (styled, not `window.confirm`)
  - Search/filter by code name
  - Status badge (active/expired/depleted)
- [ ] Service: `promo.service.ts` — add `createPromo()`, `updatePromo()`, `getPromos()`, `getPromo()`, `deletePromo()`
- [ ] Header: Add "Promos" link in admin nav
- [ ] Types: Add `PromoCreateRequest` to promo.types.ts if missing

**Backend endpoints consumed:**
- `POST /api/promos` — create
- `PUT /api/promos/{id}` — update
- `GET /api/promos` — list
- `GET /api/promos/{id}` — get
- `DELETE /api/promos/{id}` — delete

**Estimated effort:** 4-6 hours

---

### H2: Admin Commission Management

**Gap:** 2 backend endpoints with zero UI.  
**Business Impact:** Commission rates are revenue-critical; can only be set via direct API calls.  
**Branch:** `ui/high/admin-commissions`

**Deliverables:**
- [ ] Route: `/admin/commissions` (lazy loaded, ADMIN role guard)
- [ ] Page: `AdminCommissions.tsx`
  - Vendor list with current commission rate (basis points) and flat fee (cents)
  - Inline edit or edit modal for each vendor
  - Show calculated percentage (e.g., "2.50%") from basis points
  - Vendor search/filter
- [ ] Service: `admin.service.ts` — add `getCommission(vendorId)`, `setCommission(vendorId, {commissionRateBps, flatFeeCents})`
- [ ] Header: Add "Commissions" link in admin nav

**Backend endpoints consumed:**
- `GET /api/admin/commissions/{vendorId}`
- `PUT /api/admin/commissions/{vendorId}`

**Estimated effort:** 3-4 hours

---

### H3: AdminReporting — Revenue & Delivery Metrics

**Gap:** Frontend service methods exist but the AdminReporting page only uses 2 of 4 available endpoints.  
**Business Impact:** Admins miss revenue trends and delivery performance data.  
**Branch:** `ui/high/admin-reporting-complete`

**Deliverables:**
- [ ] Revenue Breakdown section in AdminReporting
  - Daily/weekly revenue data displayed as a visual chart (bar or line)
  - Period selector (daily, weekly, monthly)
  - Total revenue summary card
- [ ] Delivery Times section in AdminReporting
  - Average, min, max delivery times
  - Percentile breakdown (p50, p90, p95)
  - Stats cards with icons
- [ ] Wire `analyticsService.getRevenueReport()` and `analyticsService.getDeliveryTimes()` into the page
- [ ] Improve existing KPI cards with better visual hierarchy

**Backend endpoints consumed:**
- `GET /api/admin/reports/revenue` (already in analytics.service.ts)
- `GET /api/admin/reports/delivery-times` (already in analytics.service.ts)

**Estimated effort:** 3-4 hours

---

### H4: Checkout — Schedule Validation

**Gap:** Customers can pick invalid schedule times that only fail at order creation.  
**Business Impact:** Failed scheduled orders → frustrated customers.  
**Branch:** `ui/high/checkout-schedule-validation`

**Deliverables:**
- [ ] Service: `order.service.ts` — add `validateSchedule(dto)`
- [ ] Checkout: Call `validateSchedule` when user picks a schedule time
  - Show inline validation error if vendor is closed, time is in past, or outside operating hours
  - Disable "Place Order" button until schedule is valid
- [ ] Add loading state during validation
- [ ] Clear validation when schedule time changes

**Backend endpoints consumed:**
- `POST /api/orders/validate-schedule`

**Estimated effort:** 2-3 hours

---

## 3. MEDIUM Priority — Should Have

### M1: Admin Review Moderation

**Gap:** 2 hide endpoints with zero UI.  
**Business Impact:** Offensive/fake reviews cannot be moderated.  
**Branch:** `ui/medium/admin-reviews`

**Deliverables:**
- [ ] Route: `/admin/reviews` (lazy loaded, ADMIN role guard)
- [ ] Page: `AdminReviewModeration.tsx`
  - Two tabs: Vendor Reviews | Driver Reviews
  - Browse all reviews with: rating, comment, author, date, vendor/driver name
  - "Hide" action button for each review
  - Filter by rating threshold (e.g., show only 1-2 star reviews)
  - Search by vendor/driver name
- [ ] Service: `admin.service.ts` — add `hideVendorReview(reviewId)`, `hideDriverReview(reviewId)`
- [ ] Header: Add "Reviews" link in admin nav

**Backend endpoints consumed:**
- `PUT /api/admin/reviews/{reviewId}/hide`
- `PUT /api/admin/driver-reviews/{reviewId}/hide`
- `GET /api/vendors/{vendorId}/reviews` (browse)
- `GET /api/drivers/{driverId}/reviews` (browse)

**Estimated effort:** 4-5 hours

---

### M2: Vendor Scheduled Orders Tab

**Gap:** Backend endpoint exists but VendorDashboard has no scheduled orders tab.  
**Business Impact:** Vendors can't prepare for future scheduled orders.  
**Branch:** `ui/medium/vendor-scheduled-orders`

**Deliverables:**
- [ ] VendorDashboard: Add "Scheduled" tab (7th tab)
  - List of upcoming scheduled orders sorted by `scheduledTime`
  - Countdown timer for each (e.g., "in 2h 30m")
  - Accept/view actions
  - Auto-refresh every 60 seconds
- [ ] Service: `vendor.service.ts` — add `getScheduledOrders(vendorId)`

**Backend endpoints consumed:**
- `GET /api/vendors/{vendorId}/scheduled-orders`

**Estimated effort:** 2-3 hours

---

### M3: ChatWindow WebSocket Upgrade

**Gap:** Chat uses 5-second REST polling; backend already publishes to STOMP topic.  
**Business Impact:** 5-second message delay, unnecessary server load.  
**Branch:** `ui/medium/chat-websocket`

**Deliverables:**
- [ ] Create `useChatMessages(roomId)` hook (similar pattern to `useOrderUpdates`)
  - STOMP subscription to `/topic/chat.{roomId}`
  - REST polling fallback (existing behavior)
  - Configurable via `VITE_USE_WEBSOCKET`
- [ ] Refactor ChatWindow to use new hook
- [ ] Remove setInterval polling when WebSocket is active
- [ ] Add connection status indicator in chat header

**Estimated effort:** 3-4 hours

---

### M4: VendorDashboard WebSocket for KDS

**Gap:** Vendor order feed uses 10-second REST polling; backend publishes to STOMP `/topic/vendors.{vendorId}.orders`.  
**Business Impact:** New orders appear with up to 10-second delay in kitchen.  
**Branch:** `ui/medium/vendor-websocket`

**Deliverables:**
- [ ] Create `useVendorOrders(vendorId)` hook with STOMP subscription
  - Subscribe to `/topic/vendors.{vendorId}.orders`
  - REST polling fallback
  - Sound notification on new order (browser Audio API)
- [ ] Refactor VendorDashboard orders & KDS tabs to use hook
- [ ] Add visual + audio new-order alert

**Estimated effort:** 3-4 hours

---

### M5: Modifier Group/Modifier Editing

**Gap:** Vendors can create/delete modifiers but cannot edit existing ones.  
**Business Impact:** Vendors must delete and recreate modifiers to change names or prices.  
**Branch:** `ui/medium/modifier-editing`

**Deliverables:**
- [ ] Service: `modifier.service.ts` — add `updateModifierGroup(groupId, dto)`, `updateModifier(modifierId, dto)`
- [ ] VendorMenuManagement: Add edit button (pencil icon) on each modifier group and modifier
- [ ] Edit modal with pre-populated fields (name, price, min/max selections)
- [ ] Optimistic UI update on save

**Backend endpoints consumed:**
- `PUT /api/modifier-groups/{groupId}`
- `PUT /api/modifiers/{modifierId}`

**Estimated effort:** 2-3 hours

---

### M6: Accessibility Fixes

**Gap:** 8 accessibility issues found in audit.  
**Branch:** `ui/medium/accessibility`

**Deliverables:**
- [ ] VendorList: Add `aria-label="Search restaurants"` to search input
- [ ] ChatWindow: Add `aria-label="Type a message"` to message input
- [ ] MenuItemCard: Add `aria-label` with item name to "Add to Cart" button (e.g., `aria-label="Add Paneer Tikka to cart"`)
- [ ] StarRating: Add `aria-valuemin`, `aria-valuemax`, `aria-valuenow` in interactive mode, `role="slider"`
- [ ] Admin tables: Add `scope="col"` to `<th>` elements
- [ ] Replace all `window.confirm()` calls with accessible confirmation modals
  - Add focus trap (`onKeyDown` Escape handling)
  - `aria-modal="true"`, `role="dialog"`
- [ ] Form inputs: Add `aria-describedby` linking to error messages for field-level validation
- [ ] Audit orange text (`#f97316`) on white backgrounds for WCAG AA contrast

**Estimated effort:** 3-4 hours

---

### M7: Vendor Reports Tab

**Gap:** VendorReportController (3 endpoints) is completely unused.  
**Branch:** `ui/medium/vendor-reports`

**Deliverables:**
- [ ] VendorDashboard: Add "Reports" sub-section or integrate into Analytics tab
  - Revenue time-series display
  - Vendor KPIs (from reports endpoint, may differ from analytics)
  - CSV report export button
- [ ] Service: Add `getVendorKpis(vendorId, period)`, `getVendorRevenue(vendorId, period)`, `exportVendorReport(vendorId, period)` to a vendor reports service

**Backend endpoints consumed:**
- `GET /api/vendors/{vendorId}/reports/kpis`
- `GET /api/vendors/{vendorId}/reports/revenue`
- `GET /api/vendors/{vendorId}/reports/export`

**Estimated effort:** 3-4 hours

---

## 4. LOW Priority — Nice to Have

### L1: Skeleton Loading States

**Branch:** `ui/low/skeleton-loading`

**Deliverables:**
- [ ] Create `<Skeleton>` component (rectangular, circular, text-line variants)
- [ ] Replace `<LoadingSpinner>` with skeleton screens on: VendorList, VendorDetail, MyOrders, Notifications, AdminManagement, AdminReporting
- [ ] Use Tailwind `animate-pulse` with gray placeholders matching the content layout

**Estimated effort:** 3-4 hours

---

### L2: Empty States

**Branch:** `ui/low/empty-states`

**Deliverables:**
- [ ] Create `<EmptyState>` component (icon, title, description, optional CTA button)
- [ ] Add empty states to: Orders list, Favorites, Notifications, Cart, Vendor search results, Chat rooms, Admin tables
- [ ] Use contextual illustrations/icons (e.g., shopping bag for empty orders, heart for no favorites)

**Estimated effort:** 2-3 hours

---

### L3: Advanced Vendor Filters

**Branch:** `ui/low/vendor-filters`

**Deliverables:**
- [ ] VendorList: Add filter sidebar/toolbar with:
  - Cuisine type filter (chips/checkboxes)
  - Rating filter (minimum stars)
  - Sort options (rating, name, newest)
- [ ] Persist filters in URL query params
- [ ] Mobile: Collapsible filter drawer

**Estimated effort:** 3-4 hours

---

### L4: Confirmation Dialog Component

**Branch:** `ui/low/confirmation-dialog`

**Deliverables:**
- [ ] Create `<ConfirmDialog>` component with:
  - Title, message, confirm/cancel buttons
  - Danger variant (red confirm button for destructive actions)
  - `aria-modal`, focus trap, Escape key
  - Backdrop click to dismiss
- [ ] Replace all `window.confirm()` calls across the codebase

**Estimated effort:** 2-3 hours

---

### L5: Dark Mode

**Branch:** `ui/low/dark-mode`

**Deliverables:**
- [ ] Settings page: Dark mode toggle
- [ ] Implement Tailwind `darkMode: 'class'` strategy
- [ ] Persist preference in localStorage
- [ ] Theme all major components (Header, Footer, cards, tables, forms, modals)
- [ ] Respect `prefers-color-scheme` system preference as default

**Estimated effort:** 6-8 hours

---

### L6: Breadcrumbs Navigation

**Branch:** `ui/low/breadcrumbs`

**Deliverables:**
- [ ] Create `<Breadcrumbs>` component
- [ ] Add to: VendorDetail, OrderTrack, AdminOrderTimeline, all admin sub-pages
- [ ] Auto-generate from route hierarchy
- [ ] `aria-label="Breadcrumb"` with `<nav>` wrapper

**Estimated effort:** 2-3 hours

---

### L7: Image Lazy Loading & Placeholders

**Branch:** `ui/low/image-optimization`

**Deliverables:**
- [ ] Add `loading="lazy"` to all `<img>` tags (menu item, vendor logo, proof photos)
- [ ] Create `<OptimizedImage>` component with:
  - Blur-up placeholder
  - Error fallback image
  - Proper `alt` text enforcement
- [ ] Add `decoding="async"` for non-critical images

**Estimated effort:** 2-3 hours

---

### L8: Driver Order Assignment (Admin)

**Branch:** `ui/low/admin-driver-assignment`

**Deliverables:**
- [ ] AdminOrderTimeline or AdminManagement: Add "Assign Driver" action for unassigned orders
- [ ] Driver picker dropdown/modal
- [ ] Service: `order.service.ts` — add `assignDriver(orderId, driverId)`

**Backend endpoints consumed:**
- `POST /api/orders/{id}/assign/{driverId}`

**Estimated effort:** 2-3 hours

---

### L9: Notification Bell — WebSocket Upgrade

**Branch:** `ui/low/notification-websocket`

**Deliverables:**
- [ ] Subscribe to a user-specific STOMP topic for real-time notification delivery
- [ ] Keep REST polling as fallback
- [ ] Reduce polling interval from 30s when WebSocket is active

**Note:** Backend may need a new STOMP topic (e.g., `/topic/users.{userId}.notifications`). This is the only item that may require a backend change.

**Estimated effort:** 2-3 hours

---

## 5. Implementation Phases

### Phase 1: HIGH Priority (Days 1-3)

| Day | Task | Branch |
|-----|------|--------|
| 1 | H1: Admin Promos page | `ui/high/admin-promos` |
| 1 | H2: Admin Commissions page | `ui/high/admin-commissions` |
| 2 | H3: AdminReporting upgrade | `ui/high/admin-reporting-complete` |
| 2 | H4: Checkout schedule validation | `ui/high/checkout-schedule-validation` |
| 3 | Integration testing, merge HIGH PRs | — |

### Phase 2: MEDIUM Priority (Days 4-6)

| Day | Task | Branch |
|-----|------|--------|
| 4 | M1: Admin Review Moderation | `ui/medium/admin-reviews` |
| 4 | M2: Vendor Scheduled Orders | `ui/medium/vendor-scheduled-orders` |
| 5 | M3: Chat WebSocket | `ui/medium/chat-websocket` |
| 5 | M4: Vendor KDS WebSocket | `ui/medium/vendor-websocket` |
| 5 | M5: Modifier editing | `ui/medium/modifier-editing` |
| 6 | M6: Accessibility fixes | `ui/medium/accessibility` |
| 6 | M7: Vendor Reports tab | `ui/medium/vendor-reports` |

### Phase 3: LOW Priority (Days 7-10)

| Day | Task | Branch |
|-----|------|--------|
| 7 | L1: Skeleton loading | `ui/low/skeleton-loading` |
| 7 | L2: Empty states | `ui/low/empty-states` |
| 7 | L4: Confirmation dialog | `ui/low/confirmation-dialog` |
| 8 | L3: Advanced vendor filters | `ui/low/vendor-filters` |
| 8 | L6: Breadcrumbs | `ui/low/breadcrumbs` |
| 8 | L7: Image optimization | `ui/low/image-optimization` |
| 9 | L5: Dark mode | `ui/low/dark-mode` |
| 10 | L8: Driver assignment | `ui/low/admin-driver-assignment` |
| 10 | L9: Notification WebSocket | `ui/low/notification-websocket` |

---

## 6. Design System Standards

All new components must follow these standards:

### 6.1 Color Palette (Tailwind)

```
Primary:    orange-500 (#f97316) — buttons, links, active states
Primary-dk: orange-600 (#ea580c) — hover states
Accent:     blue-500 (#3b82f6) — info badges, links
Success:    green-500 (#22c55e) — success toasts, status badges
Warning:    amber-500 (#f59e0b) — warning states
Danger:     red-500 (#ef4444) — errors, destructive actions
Neutral:    gray-50 to gray-900 — backgrounds, text, borders
```

### 6.2 Typography

```
Headings:  font-bold text-gray-900 (text-xl to text-3xl)
Body:      text-gray-700 (text-sm to text-base)
Caption:   text-gray-500 text-xs
Monospace: font-mono text-sm (codes, IDs)
```

### 6.3 Spacing

```
Page padding:    px-4 py-6 (mobile) → px-8 py-8 (desktop)
Card padding:    p-4 → p-6
Section gap:     space-y-6
Grid gap:        gap-4 → gap-6
```

### 6.4 Component Patterns

| Pattern | Standard |
|---------|----------|
| **Cards** | `bg-white rounded-lg shadow-sm border border-gray-200 p-4` |
| **Buttons (primary)** | `bg-orange-500 hover:bg-orange-600 text-white font-semibold py-2 px-4 rounded-lg transition-colors` |
| **Buttons (secondary)** | `bg-gray-100 hover:bg-gray-200 text-gray-700 font-medium py-2 px-4 rounded-lg` |
| **Buttons (danger)** | `bg-red-500 hover:bg-red-600 text-white font-semibold py-2 px-4 rounded-lg` |
| **Inputs** | `w-full border border-gray-300 rounded-lg px-3 py-2 focus:ring-2 focus:ring-orange-500 focus:border-orange-500` |
| **Tables** | `w-full text-left border-collapse` with `border-b border-gray-200` rows |
| **Badges** | `inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium` |
| **Modals** | Fixed overlay `bg-black/50`, centered `bg-white rounded-xl shadow-xl max-w-lg w-full p-6` |
| **Tabs** | `border-b border-gray-200` with active = `border-orange-500 text-orange-600 font-semibold` |

### 6.5 Accessibility Requirements

- All interactive elements must have `aria-label` when icon-only
- All form inputs must have associated `<label>` or `aria-label`
- All modals must have `role="dialog"`, `aria-modal="true"`, focus trap, Escape key dismissal
- All tables must have `scope="col"` on `<th>` elements
- Color contrast must meet WCAG AA (4.5:1 for normal text, 3:1 for large text)
- All images must have descriptive `alt` text
- Keyboard navigation must work for all interactive components
- `aria-live="polite"` for dynamic content updates

### 6.6 Loading & Error States

Every data-fetching page/component MUST implement:

1. **Loading:** Skeleton screen (preferred) or `<LoadingSpinner />` with `aria-label`
2. **Error:** Error card with message + retry button
3. **Empty:** `<EmptyState />` with contextual message + CTA
4. **Success:** Toast notification for mutations

---

## 7. Component Implementation Checklist

For each component in the plan, verify before merging:

- [ ] Component renders correctly at all breakpoints (320px, 768px, 1024px, 1440px)
- [ ] All API calls have loading, error, and empty states
- [ ] All form inputs have labels/aria-labels
- [ ] All interactive elements are keyboard accessible (Tab, Enter, Escape)
- [ ] No `window.confirm()` or `window.alert()` used
- [ ] Toast notifications for success/error on mutations
- [ ] Types are properly defined (no `any`)
- [ ] Component is lazy-loaded if it's a page/route
- [ ] Route is protected with `<ProtectedRoute>` and correct role
- [ ] Navigation link added to Header for the correct role
- [ ] No console.log statements in production code
- [ ] Existing features still work (regression test)

---

## 8. Testing & Verification Plan

### 8.1 Manual Testing Matrix

| Scenario | Steps | Expected |
|----------|-------|----------|
| Admin creates promo | Login as admin → Promos → Create → fill form → save | Promo appears in list |
| Admin edits commission | Admin → Commissions → edit vendor rate → save | Rate updates, vendor sees new commission |
| Admin views revenue | Admin → Reports → Revenue tab | Chart renders with daily data |
| Admin moderates review | Admin → Reviews → Hide review | Review hidden from public |
| Customer schedules order | Checkout → enable schedule → pick time → validate | Invalid times show error inline |
| Vendor views scheduled | Vendor Dashboard → Scheduled tab | Future orders listed with countdown |
| Chat real-time | Open chat → send message from other user | Message appears instantly (no 5s delay) |
| Vendor KDS real-time | Submit new order → check vendor dashboard | Order appears immediately with sound |
| Modifier edit | Vendor → Menu → Modifier → Edit → change price → save | Price updates |
| Accessibility | Navigate with keyboard only | All features reachable, focus visible |

### 8.2 Automated Testing

| Type | Tool | Target |
|------|------|--------|
| Component | Vitest + React Testing Library | All new pages/components |
| E2E | Cypress | Critical flows (admin promos, checkout schedule) |
| Accessibility | axe-core / Lighthouse | All pages score ≥ 90 |
| Visual regression | Optional: Chromatic / Percy | Prevent unintended visual changes |

### 8.3 Build Verification

```bash
# Must pass before merge
cd frontend
npm run build        # Zero errors, zero warnings (except vendor chunk size)
npx tsc --noEmit     # Zero type errors
```

---

## 9. Release & Rollback Plan

### 9.1 Branch Strategy

```
main (stable)
  └── ui/high/admin-promos
  └── ui/high/admin-commissions
  └── ui/high/admin-reporting-complete
  └── ui/high/checkout-schedule-validation
  └── ui/medium/admin-reviews
  └── ui/medium/vendor-scheduled-orders
  └── ui/medium/chat-websocket
  └── ui/medium/vendor-websocket
  └── ui/medium/modifier-editing
  └── ui/medium/accessibility
  └── ui/medium/vendor-reports
  └── ui/low/skeleton-loading
  └── ui/low/empty-states
  └── ui/low/vendor-filters
  └── ui/low/confirmation-dialog
  └── ui/low/dark-mode
  └── ui/low/breadcrumbs
  └── ui/low/image-optimization
  └── ui/low/admin-driver-assignment
  └── ui/low/notification-websocket
```

### 9.2 Merge Order

1. Merge HIGH branches first (one at a time, test after each)
2. Tag `v2.1.0-high` after all HIGH merges
3. Merge MEDIUM branches
4. Tag `v2.2.0-medium` after all MEDIUM merges
5. Merge LOW branches
6. Tag `v2.3.0-low` after all LOW merges

### 9.3 Rollback

- Each feature is a separate branch → individual revert possible
- Frontend-only changes → backend remains stable
- Git revert: `git revert <merge-commit-sha>`
- Emergency: redeploy previous tag

### 9.4 Feature Flag Integration

For risky features, wrap in existing feature flag system:
```typescript
// Check feature flag before rendering
const flags = await adminService.getFeatureFlags();
if (flags['ADMIN_PROMO_MANAGEMENT']) {
  // render AdminPromos link in Header
}
```

---

## Appendix: Effort Summary

| Priority | Items | Total Estimated Hours |
|----------|-------|----------------------|
| HIGH | 4 | 12-17 hours |
| MEDIUM | 7 | 20-27 hours |
| LOW | 9 | 22-31 hours |
| **TOTAL** | **20** | **54-75 hours** |

---

## Approval

- [ ] Product owner reviewed gap analysis
- [ ] Engineering lead approved priority order
- [ ] Design review for new pages (Admin Promos, Commissions, Review Moderation)
- [ ] Approved to proceed with Phase 1 (HIGH priority)
