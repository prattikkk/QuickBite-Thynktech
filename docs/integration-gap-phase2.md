# Integration Gap Analysis — Phase 2 (Operations & Observability)

**Date:** 2026-02-21  
**Branch:** `phase2/observability`  
**Baseline:** `phase1/reliability` (commit 0d18628)

---

## 1. Current State Summary

### Backend
- Spring Boot 3.2.2, Java 17 (CI uses 21), PostgreSQL 13, Flyway V1–V10
- `spring-boot-starter-actuator` + `micrometer-registry-prometheus` deps present
- Actuator exposed: `health, info, metrics, prometheus` — **no custom metrics registered**
- Logging: plain text via `application.properties` pattern (`%d - %msg%n`) — no logback-spring.xml
- No request correlation (MDC), no structured JSON logs
- No feature flag mechanism
- No admin health/dashboard endpoint beyond `/api/health` (basic UP status)

### Frontend
- React 18 + TypeScript + Vite, axios with JWT + idempotency interceptors
- Admin routes: only `/admin/orders/:orderId/timeline`
- No admin dashboard/health page
- No request-id propagation from frontend

---

## 2. Gaps to Close in Phase 2

### 2.1 Structured JSON Logging + Correlation ID

| Gap | Details |
|-----|---------|
| No `logback-spring.xml` | Logging format entirely in `application.properties`; no JSON encoder |
| No correlation filter | No MDC-based `requestId` / `correlationId` in request lifecycle |
| No user context in logs | `userId` / `orderId` not pushed to MDC |
| Error responses lack requestId | `GlobalExceptionHandler` returns messages without trace reference |
| Frontend has no request-id | Axios interceptor doesn't generate or pass `X-Request-Id` |

### 2.2 Metrics & Alerting

| Gap | Details |
|-----|---------|
| No custom business metrics | `MeterRegistry` never injected; no `@Timed`, `@Counted` |
| Missing: `order_created_total` | No counter for order creation |
| Missing: `order_status_transitions` | No counter per status transition |
| Missing: `payment_intent_total` / `payment_success` / `payment_failed` | No payment counters |
| Missing: `webhook_processed` / `webhook_failed` / `webhook_dlq` | No webhook counters |
| Missing: request latency histograms | No `TimedAspect` bean or manual timers |
| `monitoring/` has Prometheus config | But no Grafana dashboard JSON provisioned as code |
| Alert rules reference metrics not yet emitted | e.g., `quickbite_payment_webhook_failures_total` |

### 2.3 Feature Flags

| Gap | Details |
|-----|---------|
| No feature flag mechanism | No config, DB table, or external client (Unleash/LaunchDarkly) |
| Risky features not gated | Driver auto-assign, promo engine (future) have no toggle |
| No runtime toggle API | Cannot change flags without redeploy |

### 2.4 Admin Health Dashboard

| Gap | Details |
|-----|---------|
| `/api/health` is minimal | Only returns `{"status":"UP"}` |
| No `/api/admin/health-summary` | No endpoint for queue depths, failed webhooks, DB pool stats, transition errors |
| No `AdminHealth.tsx` frontend page | Admin has no operational dashboard |
| No admin nav/landing | ADMIN role redirects nowhere useful (RoleBasedRedirect defaults to `/`) |

---

## 3. Implementation Plan

### Backend Changes
1. **`logback-spring.xml`** — JSON structured output via Logback `LogstashEncoder` (add `logstash-logback-encoder` dep)
2. **`CorrelationFilter.java`** — servlet filter: read/generate `X-Request-Id`, push to MDC (`requestId`, `userId`)
3. **`MetricsConfig.java`** — register `TimedAspect` bean; custom counters via `MeterRegistry`
4. **Instrument `OrderService`** — counters: `orders.created`, `orders.transitions`; timer: `orders.create.duration`
5. **Instrument `PaymentService`** — counters: `payments.intent.created`, `payments.success`, `payments.failed`
6. **Instrument `WebhookProcessorService`** — counters: `webhooks.processed`, `webhooks.failed`, `webhooks.dlq`
7. **`FeatureFlagService.java`** — simple map-based flags loaded from `application.properties` + env override; runtime toggle via admin API
8. **`AdminHealthController.java`** — `GET /api/admin/health-summary` aggregating DB pool, webhook queue, DLQ count, recent errors
9. **Flyway V11** — `feature_flags` table for persistent flag storage
10. **Update `SecurityConfig`** — permit actuator/prometheus, permit admin endpoints

### Frontend Changes
1. **`AdminHealth.tsx`** — dashboard page consuming `/api/admin/health-summary`
2. **`admin.service.ts`** — add `getHealthSummary()` method
3. **`App.tsx`** — add `/admin/health` route
4. **`api.ts`** — add `X-Request-Id` header generation in request interceptor

### Tests
- `CorrelationFilterTest` — verify MDC propagation
- `FeatureFlagServiceTest` — toggle, default, override
- `AdminHealthControllerTest` — response shape
- `MetricsIntegrationTest` — verify counters increment

---

## 4. Files to Create/Modify

### New Files
- `backend/src/main/resources/logback-spring.xml`
- `backend/src/main/java/com/quickbite/common/filter/CorrelationFilter.java`
- `backend/src/main/java/com/quickbite/common/config/MetricsConfig.java`
- `backend/src/main/java/com/quickbite/common/feature/FeatureFlagService.java`
- `backend/src/main/java/com/quickbite/common/feature/FeatureFlag.java`
- `backend/src/main/java/com/quickbite/common/feature/FeatureFlagRepository.java`
- `backend/src/main/java/com/quickbite/common/controller/AdminHealthController.java`
- `backend/src/main/java/com/quickbite/common/controller/FeatureFlagController.java`
- `backend/src/main/resources/db/migration/V11__add_feature_flags.sql`
- `backend/src/test/java/com/quickbite/common/filter/CorrelationFilterTest.java`
- `backend/src/test/java/com/quickbite/common/feature/FeatureFlagServiceTest.java`
- `monitoring/grafana-dashboard.json`
- `frontend/src/pages/AdminHealth.tsx`

### Modified Files
- `backend/pom.xml` — add `logstash-logback-encoder`
- `backend/src/main/resources/application.properties` — feature flag defaults, logging adjustments
- `backend/src/main/java/com/quickbite/auth/security/SecurityConfig.java` — permit new endpoints
- `backend/src/main/java/com/quickbite/orders/service/OrderService.java` — inject MeterRegistry, instrument methods
- `backend/src/main/java/com/quickbite/payments/service/PaymentService.java` — inject MeterRegistry, instrument methods
- `backend/src/main/java/com/quickbite/payments/service/WebhookProcessorService.java` — inject MeterRegistry, instrument methods
- `backend/src/main/java/com/quickbite/common/exception/GlobalExceptionHandler.java` — include requestId in responses
- `frontend/src/services/api.ts` — add X-Request-Id
- `frontend/src/services/admin.service.ts` — add getHealthSummary
- `frontend/src/App.tsx` — add admin health route
