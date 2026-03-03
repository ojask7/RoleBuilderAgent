#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# API smoke test — verifies all major endpoints respond correctly.
# Usage: ./scripts/smoke-test.sh [base_url]
# ---------------------------------------------------------------------------
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -f "${ROOT}/.env" ] && source "${ROOT}/.env"

BASE="${1:-http://localhost:${AGENT_API_PORT:-8080}}"
USER="${AGENT_API_USERNAME:-agent}"
PASS="${AGENT_API_PASSWORD:-agent-secret}"
PASS_COUNT=0
FAIL_COUNT=0

check() {
  local label="$1"
  local method="$2"
  local path="$3"
  local body="${4:-}"

  local url="${BASE}${path}"
  local http_code

  if [ "$method" = "GET" ]; then
    http_code=$(curl -sf -o /dev/null -w "%{http_code}" -u "${USER}:${PASS}" "$url" 2>/dev/null || echo "000")
  else
    http_code=$(curl -sf -o /dev/null -w "%{http_code}" -u "${USER}:${PASS}" \
      -X "$method" -H "Content-Type: application/json" -d "$body" "$url" 2>/dev/null || echo "000")
  fi

  if [[ "$http_code" =~ ^2[0-9][0-9]$ ]]; then
    echo "  PASS  ${label} (${method} ${path}) -> ${http_code}"
    ((PASS_COUNT++))
  else
    echo "  FAIL  ${label} (${method} ${path}) -> ${http_code}"
    ((FAIL_COUNT++))
  fi
}

echo "============================================"
echo "  Smoke Test — ${BASE}"
echo "============================================"
echo ""

echo "--- Health ---"
check "Actuator health"       GET  /actuator/health

echo ""
echo "--- Entitlements ---"
check "Entitlement summary"   GET  /api/v1/entitlements/summary
check "List entitlements"     GET  "/api/v1/entitlements?status=ORPHAN&page=0&size=10"

echo ""
echo "--- IT Roles ---"
check "List IT Roles"         GET  "/api/v1/roles/it?status=ACTIVE"

echo ""
echo "--- Business Roles ---"
check "List Business Roles"   GET  "/api/v1/roles/business?department=Finance"

echo ""
echo "--- Access Bundles ---"
check "Compliance dashboard"  GET  /api/v1/bundles/compliance/dashboard

echo ""
echo "============================================"
echo "  Results: ${PASS_COUNT} passed, ${FAIL_COUNT} failed"
echo "============================================"

[ "$FAIL_COUNT" -eq 0 ] && exit 0 || exit 1
