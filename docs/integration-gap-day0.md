# QuickBite Integration Gap Analysis — Day 0

> Generated: 2026-02-20  
> Scope: Full backend ↔ frontend mapping audit + missing tests

---

## 1. Endpoints Without Frontend Consumers

| # | Backend Endpoint | Method | Purpose | Gap |
|---|---|---|---|---|
| 1 | `POST /api/payments/capture` | POST | Admin/System captures payment | No frontend caller — backend auto-captures on DELIVERED, but no admin UI to manually capture |
| 2 | `POST /api/payments/refund` | POST | Vendor/Admin refunds payment | No frontend caller — backend auto-refunds on CANCELLED, but no admin/support UI |
| 3 | `POST /api/orders/{id}/assign/{driverId}` | POST | Admin/Driver manual assignment | No frontend caller — no admin assignment UI |
| 4 | `GET /api/orders/{id}/status-history` | GET | Audit trail of order status changes | No frontend caller — OrderTrack.tsx uses status from OrderDTO but doesn't fetch/display timeline |
| 5 | `POST /api/auth/refresh` | POST | Refresh JWT access token | Frontend api.ts has a TODO comment — on 401 it hard-redirects to /login instead of refreshing |
| 6 | `POST /api/auth/logout` (server-side) | POST | Revoke refresh token server-side | Frontend logout only clears localStorage; doesn't call server endpoint |
| 7 | `PATCH /api/addresses/{id}` | — | N/A | Frontend `address.service.ts` calls `PATCH /addresses/:id` but **backend has no PATCH endpoint** — only GET / POST / DELETE exist |
| 8 | `GET /actuator/health` | GET | Spring actuator health | No frontend consumer (acceptable — ops only) |

---

## 2. Frontend Calls That Mismatch Backend DTOs

| # | Frontend Call | Issue | Details |
|---|---|---|---|
| 1 | `auth.service.ts → getCurrentUser()` | **Endpoint doesn't exist** | Calls `GET /auth/me` — backend has no such endpoint. AuthenticationController only has register/login/refresh/logout |
| 2 | `address.service.ts → updateAddress()` | **HTTP method mismatch** | Calls `PATCH /addresses/:id` — backend AddressController only has GET, POST, DELETE (no PATCH/PUT) |
| 3 | `order.types.ts → PaymentStatus` | **Enum mismatch** | Frontend: `'PENDING'|'COMPLETED'|'FAILED'|'REFUNDED'`. Backend: `PENDING|AUTHORIZED|CAPTURED|FAILED|REFUNDED|CANCELLED`. Frontend uses `COMPLETED` but backend uses `AUTHORIZED` and `CAPTURED` — no mapping for `COMPLETED` |
| 4 | `auth.types.ts → UserDTO.fullName` | **Field name mismatch** | Frontend expects `fullName`, backend AuthResponse returns `name`. The `getCurrentUser` endpoint doesn't exist anyway, but the UserDTO type has wrong field name |
| 5 | `auth.types.ts → UserDTO.status` | **Field doesn't exist in response** | Frontend defines `UserStatus` type and `status` field, but backend AuthResponse doesn't return user status |
| 6 | `vendor.types.ts → VendorStatus` | **Unused enum** | `'PENDING'|'APPROVED'|'SUSPENDED'|'REJECTED'` defined but backend Vendor entity only has `active: Boolean` |
| 7 | `order.service.ts → getOrderStatusHistory` | **Response shape mismatch** | Frontend expects `Array<{ status, timestamp, note }>`. Backend returns `List<Map<String,Object>>` with keys `{ id, status, note, changedByUserId, changedAt }` — field names differ (`timestamp` vs `changedAt`) |

---

## 3. Missing Frontend Pages for Critical Endpoints

| # | Missing Page | Endpoint(s) | Impact |
|---|---|---|---|
| 1 | **Admin Dashboard** | Various admin endpoints | No admin role landing page — admin redirects to `/vendor/dashboard` |
| 2 | **Admin Order Management** | `POST /api/orders/{id}/assign/{driverId}`, `PATCH /api/orders/{id}/status` | Cannot manually assign drivers or manage order statuses |
| 3 | **Admin Payment Management** | `POST /api/payments/capture`, `POST /api/payments/refund` | Cannot manually capture/refund payments |
| 4 | **Order Timeline View** | `GET /api/orders/{id}/status-history` | No UI to view order audit trail (needed for support) |
| 5 | **User Profile / Account** | — | No user profile page for customers to view/edit their info |

---

## 4. Auth Token & Header Issues

| # | Issue | Details |
|---|---|---|
| 1 | **No token refresh implementation** | Frontend intercepts 401 and hard-redirects to `/login`. Backend has `POST /auth/refresh` but it's never called. Tokens expire → user is logged out |
| 2 | **Server-side logout not called** | `authService.logout()` only removes localStorage. Refresh tokens remain valid on server |
| 3 | **Refresh token stored in localStorage** | Should be HttpOnly cookie for security (Day 5 deliverable) |
| 4 | **WebSocket auth header** | STOMP connect sends `Authorization: Bearer <token>` but backend WebSocket config doesn't validate JWT on WS handshake — connection is `permitAll` |
| 5 | **No CSRF protection** | Stateless JWT so CSRF not strictly needed, but if cookies are used (Day 5) then CSRF must be added |

---

## 5. Missing Tests That Must Be Added for Day 1–5 Tasks

### Day 1: Reliability Refactor

| # | Test Needed | Type | Current State |
|---|---|---|---|
| 1 | Idempotency-Key duplicate POST /api/orders | Integration | **Missing** — no idempotency infrastructure exists |
| 2 | Idempotency-Key duplicate POST /api/payments/intent | Integration | **Missing** |
| 3 | Order state machine valid/invalid transitions | Unit | **Partially exists** — `OrderService.validateStatusTransition()` has logic but only 1 unit test (`OrderServiceTest`) that doesn't cover transitions |
| 4 | Role-based transition checks (only driver can mark PICKED_UP, etc.) | Unit | **Missing** — code has a TODO comment |
| 5 | Webhook duplicate event processing (idempotent) | Integration | **Missing** — webhook idempotency exists (checks `existsByProviderEventId`) but no test |
| 6 | Webhook retry/backoff/DLQ | Unit + Integration | **Missing** — no retry/queue infrastructure exists; webhooks are processed synchronously |
| 7 | Audit timeline entry on every status change | Integration | **Partially covered** — `createDeliveryStatusEntry` is called but no test verifies it |
| 8 | Admin timeline API `GET /admin/orders/{id}/timeline` | Integration | **Missing** — endpoint doesn't exist |

### Day 2: Observability

| # | Test Needed | Type | Current State |
|---|---|---|---|
| 1 | Correlation-ID in response headers | Integration | **Missing** — no correlation filter exists |
| 2 | Prometheus metrics endpoint returns expected metrics | Integration | **Missing** — no Micrometer metrics configured (actuator dependency exists but Prometheus not in POM) |
| 3 | Feature flag toggle | Unit | **Missing** — no feature flag infrastructure |
| 4 | Admin health summary endpoint | Integration | **Missing** — endpoint doesn't exist |

### Day 3: Growth Features

| # | Test Needed | Type | Current State |
|---|---|---|---|
| 1 | Favorites CRUD | Integration | **Missing** — no favorites table/entity/endpoint |
| 2 | Reorder creates correct order structure | Integration | **Missing** — no reorder endpoint |
| 3 | Promo/coupon validation + limits | Unit + Integration | **Missing** — no coupon infrastructure |
| 4 | ETA calculation | Unit | **Missing** — no ETA logic |
| 5 | Notification service dispatch | Unit | **Missing** — vendor notification is a stub log |

### Day 4: Intelligence

| # | Test Needed | Type | Current State |
|---|---|---|---|
| 1 | Vendor Busy Mode + auto-assign exclusion | Integration | **Missing** — no busy mode field |
| 2 | Driver scoring function (distance + load) | Unit | **Missing** — DriverAssignmentService does simple random/stub assignment |
| 3 | Proof of delivery (OTP/photo upload) | Integration | **Missing** — no proof infrastructure |
| 4 | Auto-escalation rules | Unit + Integration | **Missing** — no escalation logic |

### Day 5: Security + Support

| # | Test Needed | Type | Current State |
|---|---|---|---|
| 1 | Short-lived access token + refresh rotation | Integration | **Partial** — TokenStore entity exists but rotation not implemented |
| 2 | Rate limiting on auth endpoints | Integration | **Missing** — no rate limiter |
| 3 | Risk check heuristics | Unit | **Missing** — no risk flagging |
| 4 | Support console order timeline + refund | Integration | **Missing** — no support console endpoints |
| 5 | Cancellation policy engine | Unit + Integration | **Missing** — no policy engine |

---

## 6. Database Schema Gaps (for Day 1)

| Table | Exists? | Gap |
|---|---|---|
| `idempotency_keys` | ❌ No | Needed for idempotent POST endpoints |
| `webhook_events` | ✅ Yes (V5) | Missing: `attempts` (int), `last_error` (text) columns for retry tracking |
| `webhook_dlq` | ❌ No | Needed for dead-lettering failed webhook processing |
| `event_timeline` | ❌ No | Or extend `delivery_status` with `actor_role`, `event_type`, `meta` (jsonb) columns |
| `favorites` | ❌ No | Needed Day 3 |
| `coupons` / `coupon_usage` | ❌ No | Needed Day 3 |

---

## 7. WebSocket Gaps

| # | Issue | Details |
|---|---|---|
| 1 | No server-side JWT validation on WS handshake | Anyone can connect to `/ws` and subscribe to `/topic/orders.*` |
| 2 | No STOMP error handling | Client reconnects on disconnect but doesn't handle STOMP ERROR frames |
| 3 | Topic naming inconsistency | Backend publishes to `/topic/orders.{orderId}` (dot), frontend subscribes to `/topic/orders/{orderId}` (slash) — potential mismatch depending on STOMP broker config |

---

## 8. CI Gaps

| # | Issue | Details |
|---|---|---|
| 1 | CI uses JDK 17 but project uses Java 21 | `pom.xml` targets Java 21; CI `setup-java` specifies JDK 17 |
| 2 | No E2E/Postman test step in CI | Postman collection exists in `tests/postman/` but not run in CI |
| 3 | No Cypress E2E step in CI | `cypress.config.ts` exists but no CI job for it |
| 4 | No Testcontainers integration test job | Tests use `@ActiveProfiles("localtest")` which needs Docker Postgres; CI uses a service container but Testcontainers would be more isolated |

---

## Summary — Priority Fixes Before Day 1

1. **Fix CI JDK version**: 17 → 21 to match `pom.xml`
2. **Fix `auth.service.ts → getCurrentUser()`**: Remove or add backend endpoint
3. **Fix `address.service.ts → updateAddress()`**: Remove or add backend PATCH endpoint
4. **Fix `PaymentStatus` enum mismatch**: Frontend `COMPLETED` → `CAPTURED` / `AUTHORIZED`
5. **Add retry infrastructure for webhooks**: Currently synchronous, needs async queue + retry
6. **Add `idempotency_keys` table**: Required for Day 1 idempotency feature
7. **Extend `webhook_events` with retry columns**: `attempts`, `last_error`
