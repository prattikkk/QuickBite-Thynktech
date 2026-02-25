# QuickBite — Feature Coverage Matrix

> **Generated**: 2026-02-23 | **Compared Against**: Deonde-style white-label SaaS feature list  
> **Status Legend**: ✅ COMPLETE | ⚠️ PARTIAL | ❌ MISSING

---

## Customer Features

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| C1 | Vendor discovery / search | ✅ COMPLETE | `GET /vendors`, `/vendors/search` (cached) | VendorList + search bar | No cuisine/category filter | MVP |
| C2 | Vendor detail page | ⚠️ PARTIAL | `GET /vendors/{id}`, menu endpoint | VendorDetail with menu grid | No photos gallery, no reviews section | MVP |
| C3 | Menu browsing & modifiers | ⚠️ PARTIAL | MenuItems with basic fields | MenuItemCard + add-to-cart | No modifiers/extras/options | MVP |
| C4 | Cart & checkout | ✅ COMPLETE | Order creation with promo + ETA | Cart + Checkout pages | Single-vendor enforced | MVP |
| C5 | Multiple payment methods | ✅ COMPLETE | Stripe (card/UPI) + COD | CardElement + method picker | UPI uses same Stripe flow | MVP |
| C6 | Order confirmation & receipt | ✅ COMPLETE | Order number + ETA returned | Redirect to OrderTrack | No separate receipt page | MVP |
| C7 | Real-time order tracking | ✅ COMPLETE | WebSocket + HTTP polling | useOrderUpdates, progress bar | No map view | MVP |
| C8 | Order history & reorder | ✅ COMPLETE | `GET /orders`, `POST /orders/{id}/reorder` | MyOrders page + Reorder button | — | MVP |
| C9 | Ratings & reviews | ❌ MISSING | No endpoints, no entity | Rating display only (read-only) | Full build needed | Phase 2 |
| C10 | Favorites / wishlists | ✅ COMPLETE | Full CRUD `/favorites` | Favorites page + FavoriteButton | — | Phase 2 |
| C11 | Scheduled / pre-orders | ⚠️ PARTIAL | `Order.scheduledTime` field exists | No date/time picker UI | Expose in API + build UI | Phase 2 |
| C12 | Multi-address support | ✅ COMPLETE | Full CRUD `/addresses` | Address picker in Checkout | — | MVP |
| C13 | Promo codes & coupons | ✅ COMPLETE | Validate + apply + CRUD | Promo input in Checkout | No per-user limits | Phase 2 |
| C14 | Loyalty, wallet & credits | ❌ MISSING | No entity, no endpoints | No UI | Full build needed | Phase 3 |
| C15 | Multi-language & currency | ❌ MISSING | Hardcoded INR | Hardcoded English/₹ | i18n framework needed | Enterprise |
| C16 | In-app chat / support | ❌ MISSING | No chat infrastructure | No chat UI | Full build needed | Phase 2 |
| C17 | Push notifications & SMS | ⚠️ PARTIAL | In-app DB notifications only | Capacitor push plugin wired | No FCM/Twilio/SendGrid | MVP |
| C18 | Accessibility & PWA | ⚠️ PARTIAL | N/A | PWA ✅, A11y partial (aria-labels, sr-only) | Focus trapping, contrast audit | MVP |

---

## Vendor (Merchant) Features

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| V1 | Vendor portal / dashboard | ✅ COMPLETE | Vendor endpoints + order filtering | VendorDashboard (4 tabs) | — | MVP |
| V2 | Menu management + pricing | ✅ COMPLETE | Full CRUD `/menu-items` | VendorMenuManagement modal | No image upload (URL only) | MVP |
| V3 | KDS / POS integration | ⚠️ PARTIAL | Orders by vendor + status | KDS kanban tab in dashboard | No POS integration, no sound alerts | Phase 2 |
| V4 | Order acceptance/rejection | ✅ COMPLETE | `POST /accept`, `POST /reject` | Accept/Reject buttons | — | MVP |
| V5 | Opening hours + holidays | ⚠️ PARTIAL | `openHours` JSONB field | Text input (no time-picker) | No holiday/blackout, no enforcement | MVP |
| V6 | Inventory & availability | ⚠️ PARTIAL | `MenuItem.available` toggle | Toggle in menu management | No stock counts, no auto-sold-out | Phase 2 |
| V7 | Vendor performance analytics | ❌ MISSING | No analytics endpoints | No analytics UI | Full build needed | Phase 2 |
| V8 | Vendor-level promotions | ❌ MISSING | Promos are admin-only | No vendor promo UI | Extend promo system | Phase 2 |
| V9 | Commission & settlements | ❌ MISSING | No commission model | No settlement dashboard | Full build needed | Enterprise |
| V10 | Multi-location stores | ❌ MISSING | Single vendor per user | No branch management | Schema + routing changes | Enterprise |
| V11 | Vendor staff accounts | ⚠️ PARTIAL | One user per vendor | Single user manages all | Need `vendor_staff` table | MVP |
| V12 | POS / accounting integration | ❌ MISSING | No third-party connectors | N/A | Full build needed | Enterprise |

---

## Driver / Fulfillment Features

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| D1 | Driver dashboard | ✅ COMPLETE | 11 endpoints on `/drivers` | DriverDashboard (4 tabs + stats) | — | MVP |
| D2 | Accept/arrive/pickup/deliver | ✅ COMPLETE | State machine transitions | Context-aware action buttons | — | MVP |
| D3 | Live location (foreground) | ✅ COMPLETE | `PUT /location` + WS broadcast | useDriverLocation hook, GPS indicator | — | Phase 1 |
| D4 | Route & ETA calculation | ⚠️ PARTIAL | EtaService (Haversine/30kmh) | ETA display on OrderTrack | No Maps API, no routing | Phase 2 |
| D5 | Proof of delivery (OTP/photo) | ✅ COMPLETE | Photo upload + OTP generate/verify | ProofCaptureModal (photo + OTP) | — | Phase 2 |
| D6 | Earnings, shift & history | ⚠️ PARTIAL | Delivery history + shift | History tab + Start/End Shift | No earnings/incentive tracking | Phase 2 |
| D7 | Driver ratings & disputes | ❌ MISSING | No rating endpoints | No rating UI | Full build needed | Phase 2 |
| D8 | Driver incentives / bonus | ❌ MISSING | No incentive model | No incentive UI | Full build needed | Phase 3 |
| D9 | Background location (native) | ⚠️ PARTIAL | Location endpoint ready | Capacitor geolocation ✅ (foreground) | Background mode needs native config | Advanced |
| D10 | Offline mode & retries | ❌ MISSING | No offline support | PWA has basic offline page | Need request queue | Advanced |

---

## Orders, Dispatch & Routing

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| O1 | Full order lifecycle state machine | ✅ COMPLETE | `OrderStateMachine` (10 states, role guards) | Status badges + action buttons | — | MVP |
| O2 | Auto-assignment dispatch | ⚠️ PARTIAL | `DriverAssignmentService` (placeholder SQL) | N/A (backend only) | Need PostGIS / real spatial query | Phase 2 |
| O3 | Driver scoring algorithm | ❌ MISSING | Falls back to "any driver" | N/A | Need scoring (distance + load + rate) | Phase 3 |
| O4 | Manual dispatch console | ❌ MISSING | `POST /orders/{id}/assign/{driverId}` exists | No admin UI | Need admin dispatch page | Phase 2 |
| O5 | Multi-order batching | ❌ MISSING | Single order per driver | N/A | Full redesign needed | Advanced |
| O6 | Auto-escalation & SLA | ❌ MISSING | No SLA tracking | N/A | Need SLA rules + escalation engine | Phase 3 |
| O7 | ETA transparency | ⚠️ PARTIAL | Combined ETA (prep + travel) | Single ETA display | Need breakdown (prep/pickup/travel) | Phase 2 |

---

## Payments & Financials

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| P1 | Payment intents + captures | ✅ COMPLETE | Stripe PaymentIntent + capture + circuit breaker | Stripe Elements | — | MVP |
| P2 | Webhook verification + idempotency | ✅ COMPLETE | HMAC verify + dedup + retry + DLQ | N/A | — | MVP |
| P3 | Refunds | ⚠️ PARTIAL | `POST /payments/refund` exists | No admin refund UI | Need frontend | MVP |
| P4 | Settlement & reconciliation | ❌ MISSING | No settlement model | No payout dashboard | Full build needed | Enterprise |
| P5 | Commission models | ❌ MISSING | No commission entity | N/A | Full build needed | Enterprise |
| P6 | Tax calculation & invoicing | ❌ MISSING | Hardcoded 5% tax | N/A | Need tax engine | Enterprise |
| P7 | Fraud detection | ❌ MISSING | No risk rules | N/A | Full build needed | Phase 5 |
| P8 | Wallet / store credit | ❌ MISSING | No wallet entity | N/A | Full build needed | Phase 3 |

---

## Platform Ops, Observability & Reliability

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| OP1 | Structured logging + correlation IDs | ✅ COMPLETE | Logstash encoder, MDC (requestId, userId) | X-Request-Id header on all requests | — | Phase 1 |
| OP2 | Prometheus metrics | ⚠️ PARTIAL | Orders counter + transitions + duration | N/A | Need payment, cache, driver metrics | Phase 2 |
| OP3 | Grafana dashboards | ❌ MISSING | Prometheus config exists | N/A | Need dashboard definitions | Phase 2 |
| OP4 | Distributed tracing (OpenTelemetry) | ❌ MISSING | No OTEL dependencies | N/A | Full integration needed | Phase 2 |
| OP5 | Message queues (RabbitMQ/Kafka) | ❌ MISSING | Everything synchronous + @Scheduled | N/A | Need async notification, dispatch | Phase 1 |
| OP6 | Rate limiting & throttling | ✅ COMPLETE | Redis-backed, 3 tiers (general/auth/location) | N/A | — | Phase 1 |
| OP7 | Feature flags | ✅ COMPLETE | DB-backed, 10 flags, admin API | Toggle UI in AdminHealth | JVM-local cache limits multi-instance | Phase 2 |
| OP8 | Health & admin dashboard | ✅ COMPLETE | `GET /admin/health-summary` | AdminHealth page | — | Phase 2 |
| OP9 | Backup & recovery | ⚠️ PARTIAL | `ops/backup-db.sh`, `ops/restore-db.sh` | N/A | No PITR, no automated scheduling | Ops |
| OP10 | Blue/green deployments | ⚠️ PARTIAL | K8s rolling update config | N/A | No blue/green, no canary | Ops |

---

## Admin, Support & Merchant Onboarding

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| A1 | Admin console | ✅ COMPLETE | User/vendor management, health, timeline | 3 admin pages | — | MVP |
| A2 | Support console / unified timeline | ⚠️ PARTIAL | EventTimeline per order | AdminOrderTimeline page | No unified customer view, no manual actions | Phase 3 |
| A3 | Merchant onboarding + verification | ❌ MISSING | Basic vendor creation only | No KYC/docs flow | Need verification workflow | Enterprise |
| A4 | RBAC | ✅ COMPLETE | `@PreAuthorize` + 4 roles + state machine | ProtectedRoute + RoleBasedRedirect | — | MVP |
| A5 | Audit logs & activity history | ⚠️ PARTIAL | AuditLog entity exists | No audit log viewer UI | Extend coverage + build viewer | Security |
| A6 | Self-serve vendor config | ✅ COMPLETE | Vendor profile CRUD | VendorProfile component | — | MVP |

---

## Growth, Marketing & Engagement

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| G1 | Promo engine (BOGO/rules) | ⚠️ PARTIAL | Percent/Fixed discounts only | Validate at checkout | Need rule engine, BOGO, vendor promos | Phase 3 |
| G2 | Loyalty & points | ❌ MISSING | None | None | Full build needed | Phase 3 |
| G3 | Referral & affiliate | ❌ MISSING | None | None | Full build needed | Phase 3 |
| G4 | Reviews & moderation | ❌ MISSING | No review entity | Read-only rating display | Full build needed | Phase 2 |
| G5 | Email/push campaign manager | ❌ MISSING | None | None | Full build needed | Phase 3 |
| G6 | A/B testing hooks | ❌ MISSING | Feature flags exist (primitive) | None | Need analytics + experiment framework | Phase 3 |

---

## Analytics & Reporting

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| AN1 | Real-time KPIs | ❌ MISSING | Custom Micrometer counters only | No KPI dashboard | Need real-time metrics UI | Phase 2 |
| AN2 | Historical reports | ❌ MISSING | No reporting endpoints | No reports UI | Full build needed | Phase 2 |
| AN3 | Custom dashboards & export | ❌ MISSING | No export endpoints | N/A | CSV/PDF generation needed | Enterprise |
| AN4 | BI integration | ❌ MISSING | No data warehouse | N/A | ETL pipeline needed | Enterprise |

---

## Security & Compliance

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| S1 | Short-lived access + refresh rotation | ✅ COMPLETE | 15min access, 7d refresh, rotation | Token stored in localStorage | Should use HttpOnly cookies | Phase 1 |
| S2 | HttpOnly secure refresh cookies | ❌ MISSING | Refresh via JSON body | localStorage | Need cookie-based refresh | Phase 1 |
| S3 | Input validation | ✅ COMPLETE | `@Valid` + Bean Validation | Client-side validation utils | — | MVP |
| S4 | Vulnerability scanning | ❌ MISSING | No Snyk/Dependabot | N/A | CI integration needed | CI |
| S5 | Data retention / GDPR | ⚠️ PARTIAL | Export + erasure endpoints | Profile page buttons | No retention policy automation | Enterprise |
| S6 | PCI tokenization | ✅ COMPLETE | Stripe hosted elements | CardElement (never touches PAN) | — | Must-have |
| S7 | WAF / DDoS | ❌ MISSING | No WAF config | N/A | Cloud provider WAF needed | Ops |
| S8 | Password reset | ❌ MISSING | No endpoints | No UI | Need email-based reset flow | MVP |
| S9 | Email verification | ❌ MISSING | No verification | No UI | Need verification flow | MVP |

---

## Integrations & Ecosystem

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| I1 | Maps & routing (Google/Mapbox) | ❌ MISSING | Haversine only (no SDK) | No map component | Need Maps SDK | Phase 2 |
| I2 | Payment gateways (Stripe) | ✅ COMPLETE | Stripe Java SDK + webhooks | Stripe Elements | — | MVP |
| I3 | SMS / WhatsApp (Twilio) | ❌ MISSING | No SMS provider | N/A | Need Twilio/MSG91 | MVP |
| I4 | Email (SendGrid/SES) | ❌ MISSING | No email provider | N/A | Need transactional email | MVP |
| I5 | Push (FCM/APNs) | ⚠️ PARTIAL | No server-side push | Capacitor plugin wired | Need FCM server SDK | MVP |
| I6 | Analytics (Sentry/Segment) | ❌ MISSING | No crash reporting | No frontend tracking | Need Sentry + analytics | Phase 2 |
| I7 | POS / accounting | ❌ MISSING | No connectors | N/A | Full build needed | Enterprise |

---

## Developer Experience & Platform

| # | Feature | Status | Backend? | Frontend? | Needs Work? | Priority |
|---|---------|--------|----------|-----------|-------------|----------|
| DX1 | OpenAPI / SDKs | ⚠️ PARTIAL | springdoc auto-generated | N/A | Need metadata, security schemes, versioning | MVP |
| DX2 | Testcontainers + CI | ⚠️ PARTIAL | Testcontainers PostgreSQL ✅ | N/A | No CI pipeline | MVP |
| DX3 | Postman/Cypress E2E | ✅ COMPLETE | Postman collection + Cypress specs | — | — | MVP |
| DX4 | Feature flag integration | ✅ COMPLETE | DB-backed, admin toggle | Toggle UI | JVM-local cache issues | Phase 2 |
| DX5 | Modular architecture | ⚠️ PARTIAL | Package-by-feature | N/A | Need event bus, reduce coupling | Phase 1 |
| DX6 | Release runbooks | ✅ COMPLETE | `docs/runbook.md`, release notes template | N/A | — | Ops |

---

## Summary Counts

| Status | Count | Percentage |
|--------|-------|------------|
| ✅ COMPLETE | 39 | 43% |
| ⚠️ PARTIAL | 26 | 29% |
| ❌ MISSING | 26 | 29% |
| **TOTAL** | **91** | — |

### By Priority

| Priority | Complete | Partial | Missing | Total |
|----------|----------|---------|---------|-------|
| MVP | 18 | 5 | 3 | 26 |
| Phase 1 | 3 | 2 | 1 | 6 |
| Phase 2 | 2 | 8 | 14 | 24 |
| Phase 3 | 0 | 1 | 8 | 9 |
| Enterprise | 0 | 1 | 10 | 11 |
| Advanced | 0 | 1 | 3 | 4 |
| Ops | 0 | 2 | 1 | 3 |
| Other | 16 | 6 | — | 22 |
