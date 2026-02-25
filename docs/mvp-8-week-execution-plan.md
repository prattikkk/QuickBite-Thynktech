# QuickBite — 8-Week MVP Execution Plan

> **Reference**: `docs/mvp-prd-remaining-features.md`, `docs/feature-coverage-matrix.md`  
> **Team Assumption**: 2 full-stack engineers + 1 part-time DevOps  
> **Rule**: No sprint moves forward without backend implemented, frontend wired, tests passing, manual verification checklist completed.

---

## Overview

```
Week 0  ─ Foundation & DevOps Setup
Week 1  ─ Security: Password Reset + Email Verification
Week 2  ─ Email & SMS Integration
Week 3  ─ Push Notifications (FCM + Web Push)
Week 4  ─ Ratings & Reviews System
Week 5  ─ Map Integration & Live Tracking
Week 6  ─ Refund UI + Partial Completion + CI Hardening
Week 7  ─ Performance, Security Hardening & Polish
Week 8  ─ Staging Deploy, E2E Validation & Release Prep
```

---

## Week 0 — Foundation & DevOps Setup

**Goal**: CI/CD pipeline, S3 storage, development workflow.

### Backend
- [ ] Configure GitHub Actions CI workflow (`ci.yml`):
  - Checkout → Maven build → Testcontainers tests → JaCoCo report
  - Fail on coverage < 60% (raise to 70% by Week 6)
- [ ] Add Dependabot configuration (`dependabot.yml`)
- [ ] Configure Docker image builds → push to GitHub Container Registry (ghcr.io)
- [ ] Set up S3 bucket for delivery proof photos (replace local file storage)
- [ ] Add `@Async` thread pool configuration for async operations

### Frontend
- [ ] Add ESLint + Prettier CI check in GitHub Actions
- [ ] Frontend build step in CI (Vite build + TypeScript check)
- [ ] Configure Sentry SDK for frontend error tracking

### Testing
- [ ] Ensure all 246 existing tests pass in CI (Testcontainers PostgreSQL)
- [ ] Add Cypress smoke test to CI pipeline (headless Chrome)

### DevOps
- [ ] Create staging K8s namespace (`quickbite-staging`)
- [ ] Configure staging database (separate PostgreSQL instance)
- [ ] Set up SSL/TLS with cert-manager + Let's Encrypt
- [ ] Create deploy script: `deploy-staging.yml` (auto on merge to `develop`)

### Acceptance Criteria
- [ ] Every PR triggers CI (build + test + lint)
- [ ] Docker images published to ghcr.io
- [ ] S3 bucket accessible for file uploads
- [ ] Staging namespace created with database
- [ ] All existing tests green in CI

---

## Week 1 — Security: Password Reset & Email Verification

**Goal**: Users can recover accounts and verify email addresses.

### Backend
- [ ] Create Flyway V21: `password_reset_tokens` table
- [ ] Create Flyway V22: `email_verified` column + `email_verification_token` on users
- [ ] Implement `PasswordResetToken` entity + repository
- [ ] Implement `PasswordResetService`:
  - `requestReset(email)` → generate token, hash it, store, trigger email
  - `validateToken(token)` → check hash, expiry, used status
  - `resetPassword(token, newPassword)` → update user, mark token used
- [ ] `POST /api/auth/forgot-password` endpoint
- [ ] `POST /api/auth/reset-password` endpoint
- [ ] `POST /api/auth/verify-email` endpoint
- [ ] Rate limit: 3 reset requests per email per hour
- [ ] Set `email_verified = true` for all existing users (migration)
- [ ] Feature flag: `require-email-verification` (default: off)

### Frontend
- [ ] "Forgot Password?" link on `Login.tsx`
- [ ] `ForgotPassword.tsx` page — email input, submit, success message
- [ ] `ResetPassword.tsx` page — new password form, token from URL query param
- [ ] `VerifyEmail.tsx` page — token validation on mount, success/error display
- [ ] Post-registration: show "Check your email" notice
- [ ] Add routes: `/forgot-password`, `/reset-password`, `/verify-email`

### Testing
- [ ] Unit: `PasswordResetService` (token generation, expiry, rate limiting)
- [ ] Integration: Full flow with Testcontainers (request → validate → reset)
- [ ] Integration: Email verification flow
- [ ] Cypress: Forgot password → reset → login flow

### Acceptance Criteria
- [ ] User clicks "Forgot Password" → enters email → receives reset link
- [ ] Reset link works within 1 hour, fails after
- [ ] Password successfully updated, old password no longer works
- [ ] New users get verification email after registration
- [ ] Feature flag can enforce email verification before ordering
- [ ] Rate limiting prevents abuse (>3 requests/hour blocked)
- [ ] All new tests pass in CI

---

## Week 2 — Email & SMS Provider Integration

**Goal**: Transactional email and SMS delivery operational.

### Backend
- [ ] `EmailService` interface: `send(to, subject, templateName, variables)`
- [ ] `SendGridEmailService` implementation (Spring `@Service`)
- [ ] HTML templates (Thymeleaf):
  - `password-reset.html`
  - `email-verification.html`
  - `order-confirmation.html`
  - `order-status-change.html`
  - `welcome.html`
- [ ] Wire `PasswordResetService` → `EmailService`
- [ ] Wire `OrderStatusService` → `EmailService` (on status change)
- [ ] Feature flag: `email-notifications`
- [ ] `SmsService` interface: `send(phoneNumber, templateName, variables)`
- [ ] `TwilioSmsService` implementation
- [ ] SMS templates: OTP, order placed, out for delivery, delivered
- [ ] Feature flag: `sms-notifications`
- [ ] Rate limiting: max 5 SMS per user per hour
- [ ] Configuration: `SENDGRID_API_KEY`, `TWILIO_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_FROM_NUMBER`

### Frontend
- [ ] (No major frontend changes — emails are backend-triggered)
- [ ] Add phone number field to profile page (optional)
- [ ] SMS notification preference toggle in settings

### Testing
- [ ] Unit: `SendGridEmailService` (mock HTTP client, verify payloads)
- [ ] Unit: `TwilioSmsService` (mock HTTP client)
- [ ] Integration: Email templates render correctly
- [ ] Integration: Order status change triggers email + SMS
- [ ] Feature flag tests: disabled → no email/SMS sent

### Acceptance Criteria
- [ ] Password reset email received (checked in SendGrid dashboard / Mailtrap)
- [ ] Order confirmation email sent on successful checkout
- [ ] SMS sent on "Out for Delivery" status
- [ ] Feature flags can disable email/SMS independently
- [ ] Rate limiting blocks excessive SMS
- [ ] All new tests pass in CI

---

## Week 3 — Push Notifications (FCM + Web Push)

**Goal**: Real-time push notifications to mobile and browser.

### Backend
- [ ] Create Flyway V23: `device_tokens` table
- [ ] Implement `DeviceToken` entity + repository
- [ ] `POST /api/devices/register` — register FCM/APNs/Web Push token
- [ ] `DELETE /api/devices/{token}` — unregister (on logout)
- [ ] `PushNotificationService`:
  - FCM Admin SDK integration (Firebase project setup)
  - VAPID Web Push support
  - `sendToUser(userId, title, body, data)` → fetch all user tokens → send
- [ ] Hook into `NotificationService.create()` → trigger push delivery
- [ ] Handle FCM error codes: invalid token → auto-unregister
- [ ] Configuration: `FIREBASE_CREDENTIALS_JSON`, `VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`

### Frontend
- [ ] Generate VAPID key pair, add to environment config
- [ ] Service worker: `push` event handler in `sw.js`
- [ ] Service worker: `notificationclick` handler (deep link to order)
- [ ] `POST /api/devices/register` call:
  - **Web**: On permission grant, register `PushManager.getSubscription()` token
  - **Capacitor**: On `PushNotifications.addListener('registration')`, register FCM token
- [ ] Permission prompt: Show after first successful order (not on first visit)
- [ ] Notification preferences page: toggle order updates, promotions
- [ ] Unregister token on logout

### Testing
- [ ] Unit: `PushNotificationService` (mock FCM, verify payloads)
- [ ] Integration: Device registration + push trigger on order status change
- [ ] Integration: Invalid token auto-cleanup
- [ ] Manual: Test on real Android device (Capacitor build) + Chrome Web Push

### Acceptance Criteria
- [ ] PWA user grants permission → receives push on order status change
- [ ] Capacitor app receives FCM push notification
- [ ] Clicking notification opens the order tracking page
- [ ] Logout clears device token
- [ ] Invalid tokens automatically cleaned up
- [ ] Permission prompt appears only after first order
- [ ] All new tests pass in CI

---

## Week 4 — Ratings & Reviews System

**Goal**: Customers can rate vendors, ratings displayed on vendor cards.

### Backend
- [ ] Create Flyway V24: `reviews` table (unique on `order_id, customer_id`)
- [ ] Implement `Review` entity + repository
- [ ] `ReviewService`:
  - `submitReview(orderId, customerId, rating, comment)` — validate order is DELIVERED and belongs to customer
  - `getVendorReviews(vendorId, pageable)` — paginated
  - `getRatingSummary(vendorId)` — avg rating, count by star
- [ ] `POST /api/orders/{orderId}/review` (CUSTOMER role)
- [ ] `GET /api/vendors/{vendorId}/reviews` (Public)
- [ ] `GET /api/vendors/{vendorId}/rating-summary` (Public)
- [ ] Update `Vendor.rating` field: recalculate on new review (incremental average)
- [ ] Admin endpoints:
  - `PUT /api/admin/reviews/{id}/hide` — moderate (hide review)
  - `GET /api/admin/reviews/flagged` — reviews with complaints

### Frontend
- [ ] `ReviewForm.tsx` component — star selector (1-5) + comment textarea
- [ ] Show ReviewForm on `OrderTrack.tsx` after order status = DELIVERED
- [ ] `VendorReviews.tsx` section on `VendorDetail.tsx` — paginated review list
- [ ] Star rating + review count display on `VendorList.tsx` cards
- [ ] Rating badge on vendor header
- [ ] Admin: review moderation in admin panel (optional, MVP-light)

### Testing
- [ ] Unit: `ReviewService` (submit, duplicate prevention, average calculation)
- [ ] Integration: Full review flow (create order → deliver → review → verify rating)
- [ ] Integration: Pagination, vendor rating recalculation
- [ ] Cypress: Customer completes order → submits review → sees on vendor page

### Acceptance Criteria
- [ ] Customer can submit 1-5 star rating + comment after delivery
- [ ] Cannot review same order twice (409 Conflict)
- [ ] Cannot review non-delivered order (400 Bad Request)
- [ ] Vendor detail shows average rating and review list
- [ ] Vendor list cards show star rating
- [ ] New review updates vendor's average rating
- [ ] All new tests pass in CI

---

## Week 5 — Map Integration & Live Order Tracking

**Goal**: Visual map-based order tracking with driver location.

### Backend
- [ ] `MapsService` interface with `GoogleMapsService` implementation:
  - `geocode(address)` → `{lat, lng}`
  - `reverseGeocode(lat, lng)` → address string
  - `drivingDistance(origin, destination)` → `{distanceMeters, durationSeconds}`
- [ ] Enhance address creation: auto-geocode on `POST /api/addresses`
- [ ] Enhance `EtaService`: use real driving time from Maps API (cache results)
- [ ] Cache geocoding results in Redis (key: normalized address, TTL: 7 days)
- [ ] `GET /api/orders/{id}/tracking` — return vendor location, driver location, customer location, route polyline (optional)
- [ ] Configuration: `GOOGLE_MAPS_API_KEY` (or `MAPBOX_ACCESS_TOKEN`)

### Frontend
- [ ] `MapView.tsx` component using `@react-google-maps/api` or Mapbox GL JS
- [ ] `OrderTrackingMap.tsx` on `OrderTrack.tsx`:
  - Vendor marker (restaurant icon)
  - Driver marker (bike/car icon, live position from WebSocket)
  - Customer marker (pin)
  - Route polyline (vendor → driver → customer)
  - Auto-center and zoom to fit all markers
- [ ] Driver dashboard `DriverMap.tsx`:
  - Current delivery route overlay
  - Navigation link ("Open in Google Maps")
- [ ] Address creation: autocomplete search box using Places API
- [ ] Vendor list: show distance from user (if geolocation permitted)

### Testing
- [ ] Unit: `GoogleMapsService` (mock HTTP, verify caching)
- [ ] Integration: Geocoding on address creation
- [ ] Integration: ETA calculation with real driving time
- [ ] Cypress: Order tracking page shows map with markers
- [ ] Manual: Verify live driver position updates on map

### Acceptance Criteria
- [ ] Order tracking page shows vendor, driver, and customer on map
- [ ] Driver position updates in real-time via WebSocket
- [ ] ETA uses driving distance (not Haversine straight-line)
- [ ] Geocoding results cached (Redis)
- [ ] Address creation has autocomplete
- [ ] Driver can open route in Google Maps app
- [ ] Maps API key cost < $50/month at current test volume
- [ ] All new tests pass in CI

---

## Week 6 — Refund UI, Partial Completion & Test Coverage

**Goal**: Admin refund workflow, fix partial implementations, raise test bar.

### Backend
- [ ] Audit all PARTIAL features from coverage matrix — fix top 5:
  1. **Menu item availability toggle**: wire to frontend
  2. **Vendor hours enforcement**: validate on order creation (reject if vendor closed)
  3. **Scheduled order dispatch**: time-based trigger (cron job checks upcoming orders)
  4. **Per-user promo code limits**: add `user_id` to `promo_redemptions`, enforce in `PromoService`
  5. **Auto-dispatch scoring**: improve placeholder with distance-based assignment (non-PostGIS, Haversine ranking)
- [ ] Add `@Transactional(readOnly = true)` to read-only service methods
- [ ] Add missing indexes: `orders.customer_id`, `orders.vendor_id`, `orders.driver_id`
- [ ] Expand Postman/Newman collection with new endpoints

### Frontend
- [ ] Admin refund workflow:
  - Refund button on `AdminOrderTimeline.tsx`
  - Confirmation modal (amount, reason)
  - Refund status display
- [ ] Menu availability toggle on Vendor dashboard
- [ ] Scheduled order date/time UI (basic — full Phase 2)
- [ ] Fix: refresh token auto-rotation (complete the TODO)

### Testing
- [ ] Raise JaCoCo minimum to 70%
- [ ] Add missing tests for existing code:
  - `PaymentService` webhook handling edge cases
  - `OrderStateMachine` invalid transitions
  - `FeatureFlagService` with Redis
  - `RateLimitService` sliding window accuracy
- [ ] Cypress: Admin refund flow
- [ ] Cypress: Customer rating flow (from Week 4)
- [ ] Newman: Full regression suite with 50+ requests

### Acceptance Criteria
- [ ] Admin can refund an order from the UI (Stripe refund issued)
- [ ] Vendor hours enforced on order creation
- [ ] Promo codes respect per-user limits
- [ ] JaCoCo ≥ 70% in CI
- [ ] Newman collection has 50+ requests, all passing
- [ ] All existing + new tests pass in CI

---

## Week 7 — Performance, Security Hardening & Polish

**Goal**: Harden for production deployment.

### Backend
- [ ] **Security hardening**:
  - Migrate refresh token to HttpOnly secure cookie
  - Account lockout after 5 failed login attempts
  - Secure all headers (HSTS, X-Frame-Options, X-Content-Type-Options)
  - Validate Content-Type on all POST/PUT endpoints
  - Add OWASP ZAP scan to CI (informational, non-blocking)
- [ ] **Performance**:
  - Add database indexes identified in analysis
  - Enable query logging for slow queries (>500ms)
  - Optimize N+1 queries (fetch joins for orders + items + vendor)
  - Redis cache warmup on startup for frequently accessed vendors
- [ ] **Resilience**:
  - Circuit breaker for Maps API
  - Retry on email/SMS transient failures
  - Graceful degradation when Redis unavailable

### Frontend
- [ ] **Performance**:
  - Lazy load map components (React.lazy + Suspense)
  - Image optimization (WebP, lazy loading, srcset)
  - Bundle size audit (vite-plugin-visualizer)
  - Reduce bundle < 300KB gzipped
- [ ] **Accessibility**:
  - ARIA labels on all interactive elements
  - Keyboard navigation for star rating
  - Focus management on modal open/close
  - Color contrast audit (WCAG AA)
- [ ] **UX polish**:
  - Loading skeletons on all data-fetching pages
  - Error boundaries with retry buttons
  - Empty states with helpful messaging
  - Consistent toast notifications

### Testing
- [ ] Performance test with k6:
  - 100 concurrent order creations < 2s p95
  - 500 concurrent vendor list requests < 500ms p95
  - WebSocket: 200 concurrent connections stable
- [ ] Security test: OWASP ZAP baseline scan
- [ ] Accessibility: axe-core in Cypress tests

### Acceptance Criteria
- [ ] Refresh token in HttpOnly cookie (not localStorage)
- [ ] Account locks after 5 bad attempts, unlock via email
- [ ] No critical findings in OWASP ZAP scan
- [ ] Order creation p95 < 2s at 100 concurrent users
- [ ] Frontend bundle < 300KB gzipped
- [ ] axe-core reports zero critical violations
- [ ] All tests pass in CI

---

## Week 8 — Staging Deploy, E2E Validation & Release Prep

**Goal**: Full deployment to staging, validate everything works, prep for production.

### DevOps
- [ ] Deploy full system to staging K8s:
  - Backend (3 replicas) + Frontend (2 replicas) + PostgreSQL + Redis
  - Ingress with SSL
  - Environment variables configured (SendGrid, Twilio, FCM, Maps, Stripe test keys)
- [ ] Run Flyway migrations (V1-V24) on staging database
- [ ] Seed staging database with test data
- [ ] Configure Sentry for staging environment
- [ ] Configure UptimeRobot health checks

### Testing (Staging Environment)
- [ ] Run full Newman collection against staging API
- [ ] Run Cypress suite against staging frontend
- [ ] Manual test checklist:
  - [ ] Customer: register → verify email → browse → order → pay → track on map → rate
  - [ ] Vendor: login → accept order → complete order → view analytics (Phase 2 placeholder)
  - [ ] Driver: login → go online → accept delivery → navigate → deliver with photo → complete
  - [ ] Admin: login → view dashboard → manage users → refund order → view audit log
- [ ] Push notification test on real device (Android + Chrome)
- [ ] Password reset flow end-to-end
- [ ] Load test: 50 concurrent users for 5 minutes

### Documentation
- [ ] Update `README.md` with setup instructions for new developers
- [ ] Create `docs/deployment.md` update with CI/CD instructions
- [ ] Create `docs/release_notes_v1.0.md`
- [ ] API documentation (Swagger/OpenAPI export)
- [ ] Create `docs/post_deploy_checks.md` update

### Release Prep
- [ ] Create production deploy workflow (`deploy-prod.yml`) with manual approval
- [ ] Tag release: `v1.0.0-rc1`
- [ ] Generate production environment variable template
- [ ] Backup/restore scripts tested
- [ ] Rollback procedure documented and tested

### Acceptance Criteria
- [ ] All features work on staging (manual verification checklist complete)
- [ ] Newman + Cypress pass on staging
- [ ] Push notifications received on real devices
- [ ] Load test passes (50 concurrent, p95 < 2s)
- [ ] Zero critical alerts in Sentry
- [ ] API docs available at `/api/docs`
- [ ] Production deploy workflow ready (pending approval)
- [ ] Release tagged: `v1.0.0-rc1`

---

## Sprint Velocity & Risk Buffer

| Week | Estimated Story Points | Risk Level | Buffer Tasks (if ahead) |
|------|----------------------|------------|------------------------|
| W0 | 13 | Low | Extra CI checks, pre-commit hooks |
| W1 | 21 | Medium | Social login (Google OAuth) |
| W2 | 21 | Medium | Receipt PDF generation |
| W3 | 21 | High (FCM setup) | Notification preferences UI |
| W4 | 18 | Low | Driver review system |
| W5 | 26 | High (Maps API) | Address book management |
| W6 | 21 | Medium | Vendor hours UI editor |
| W7 | 21 | Medium | Dark mode toggle |
| W8 | 13 | Low | Performance optimizations |

**Total estimated**: ~155 story points / 8 weeks ≈ 19 pts/week for 2 engineers.

---

## Definition of Done (Every Sprint)

1. ✅ Backend endpoints implemented with validation + error handling
2. ✅ Frontend pages/components wired to new endpoints
3. ✅ Unit tests for all new services (≥80% method coverage)
4. ✅ Integration tests for all new endpoints
5. ✅ At least 1 Cypress E2E test for the sprint's primary flow
6. ✅ CI pipeline green (build + test + lint)
7. ✅ JaCoCo coverage ≥ threshold (60% W0-W5, 70% W6+)
8. ✅ No critical/high Dependabot alerts
9. ✅ PR reviewed and approved
10. ✅ Manual verification checklist signed off
11. ✅ Deployed to staging (from W8 onward; local Docker before that)

---

## Dependencies & External Setup

| Dependency | When Needed | Setup Time | Owner |
|-----------|------------|------------|-------|
| SendGrid account | Week 2 | 1 day | DevOps |
| Twilio account | Week 2 | 1 day | DevOps |
| Firebase project (FCM) | Week 3 | 2 days | Backend |
| VAPID key pair | Week 3 | 30 min | Backend |
| Google Maps API key | Week 5 | 1 day | DevOps |
| S3 bucket + IAM | Week 0 | 1 day | DevOps |
| Sentry project | Week 0 | 30 min | Frontend |
| K8s staging cluster | Week 0 | 2 days | DevOps |
| Domain + SSL cert | Week 0 | 1 day | DevOps |
| Stripe test keys (existing) | Already done | — | — |
