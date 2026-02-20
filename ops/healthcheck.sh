#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────
# QuickBite — Health Check Script
# Usage:   ./ops/healthcheck.sh [base-url]
# Default: http://localhost:8080
# Exit 0 if healthy, 1 if any check fails.
# ─────────────────────────────────────────────────────────────────
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
PASS=0
FAIL=0

check() {
  local name="$1"
  local url="$2"
  local expected="${3:-200}"

  STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")

  if [ "$STATUS" = "$expected" ]; then
    echo "  ✅  ${name} — HTTP ${STATUS}"
    PASS=$((PASS + 1))
  else
    echo "  ❌  ${name} — HTTP ${STATUS} (expected ${expected})"
    FAIL=$((FAIL + 1))
  fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  QuickBite Health Check"
echo "  Target: ${BASE_URL}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Core endpoints
check "Actuator Health" "${BASE_URL}/actuator/health"
check "API Health"      "${BASE_URL}/api/health"

# Auth endpoints (expect 4xx since no token)
check "Auth Login"      "${BASE_URL}/api/auth/login"       "405"
check "Auth Register"   "${BASE_URL}/api/auth/register"    "405"

# Protected endpoints (expect 401/403 without token)
check "Vendors List"    "${BASE_URL}/api/vendors"          "401"
check "Orders List"     "${BASE_URL}/api/orders"           "401"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Results: ${PASS} passed, ${FAIL} failed"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ "$FAIL" -gt 0 ]; then
  exit 1
fi
