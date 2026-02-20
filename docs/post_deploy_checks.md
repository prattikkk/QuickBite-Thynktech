# QuickBite — Post-Deploy Verification Checklist

> Run these checks **immediately** after every production deployment.
> Estimated time: 5–10 minutes.

---

## 1. Health Checks (Automated)

```bash
# Run the automated health check script
./ops/healthcheck.sh http://localhost:8080

# Expected: All checks pass (exit code 0)
```

---

## 2. API Smoke Tests

### Auth Flow
```bash
# Register a test user
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Deploy Test","email":"deploy-test@example.com","password":"Test1234!","role":"CUSTOMER"}' \
  | jq .

# Login and capture token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"deploy-test@example.com","password":"Test1234!"}' \
  | jq -r '.token')

echo "Token: ${TOKEN:0:20}..."
```

### Protected Endpoints
```bash
# Fetch vendors
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/vendors | jq '.[] | .name' | head -5

# Fetch orders (should be empty for new user)
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/orders | jq .
```

### WebSocket Connectivity
```bash
# Test WebSocket upgrade (should get 101 or connection error, not 404)
curl -s -o /dev/null -w "%{http_code}" \
  -H "Upgrade: websocket" \
  -H "Connection: Upgrade" \
  http://localhost:8080/ws/info
# Expected: 200 (SockJS info endpoint)
```

---

## 3. Database Verification

```sql
-- Connect to production DB
-- psql -h <host> -U quickbite -d quickbite

-- Check Flyway migration status
SELECT version, description, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 10;
-- ✅ All rows should have success = true

-- Check table counts
SELECT 'users' AS table_name, COUNT(*) FROM users
UNION ALL SELECT 'vendors', COUNT(*) FROM vendors
UNION ALL SELECT 'menu_items', COUNT(*) FROM menu_items
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'payments', COUNT(*) FROM payments;

-- Check for recent payment records (if applicable)
SELECT id, status, provider, amount_cents, created_at
FROM payments
ORDER BY created_at DESC
LIMIT 5;

-- Verify no orphaned records
SELECT o.id, o.user_id
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
WHERE u.id IS NULL;
-- ✅ Should return 0 rows
```

---

## 4. Frontend Verification

| Check | URL | Expected |
|-------|-----|----------|
| Home page loads | `/` | 200, React app renders |
| Login page | `/login` | 200, form visible |
| Register page | `/register` | 200, form visible |
| Vendor list | `/vendors` | 200 (may redirect to login) |
| SPA deep link | `/orders/123` | 200 (not 404) |
| Static assets | `/assets/*.js` | 200, gzip content-encoding |

```bash
# Quick check
curl -s -I http://localhost/ | head -5
curl -s -I http://localhost/vendors | head -5
curl -s -I http://localhost/nonexistent/deep/route | head -5
# All should return HTTP 200 (SPA fallback)
```

---

## 5. Performance Baseline

```bash
# Simple load test with curl (sequential)
for i in $(seq 1 10); do
  TIME=$(curl -s -o /dev/null -w "%{time_total}" http://localhost:8080/actuator/health)
  echo "Request $i: ${TIME}s"
done
# ✅ All under 500ms

# If Apache Bench is available:
ab -n 100 -c 10 http://localhost:8080/api/health
# ✅ Mean response < 200ms, 0 failed requests
```

---

## 6. Monitoring & Alerting

- [ ] Prometheus scrape target is UP: `http://localhost:9090/targets`
- [ ] Grafana dashboard loads: `http://localhost:3000`
- [ ] Key metrics are populating:
  - `http_server_requests_seconds_count`
  - `jvm_memory_used_bytes`
  - `hikaricp_connections_active`
- [ ] Alert rules are active (payment failure rate, error rate)

---

## 7. Cleanup

```bash
# Remove test user (optional, or keep for monitoring)
# DELETE FROM users WHERE email = 'deploy-test@example.com';
```

---

## Sign-Off

| Checker | Status | Time |
|---------|--------|------|
| Health checks | ⬜ Pass / ⬜ Fail | |
| API smoke tests | ⬜ Pass / ⬜ Fail | |
| Database verification | ⬜ Pass / ⬜ Fail | |
| Frontend verification | ⬜ Pass / ⬜ Fail | |
| Performance baseline | ⬜ Pass / ⬜ Fail | |
| Monitoring active | ⬜ Pass / ⬜ Fail | |

**Deployed by:** _______________
**Date/Time:** _______________
**Verdict:** ⬜ Approved / ⬜ Rollback Required

---

*Last updated: Day 6 — Production Readiness*
