#!/usr/bin/env bash
# Checks that all three services answer. Run after starting them.
BACKEND="${BACKEND_URL:-http://localhost:8080}"; ML="${ML_URL:-http://localhost:8000}"; FRONT="${FRONTEND_URL:-http://localhost:5173}"
ok=0
check() { code=$(curl -s -o /tmp/smoke.out -m 20 -w '%{http_code}' "$2" || true); if [ "$code" = "$3" ]; then echo "PASS  $1 ($code)"; else echo "FAIL  $1 (got $code, expected $3) $2"; ok=1; fi; }
check "ML service health"        "$ML/health" 200
check "Backend health"           "$BACKEND/api/v1/health" 200
check "Backend forecast (Chennai)" "$BACKEND/api/v1/weather/forecast?latitude=13.08&longitude=80.27" 200
check "Backend risk"             "$BACKEND/api/v1/risk?latitude=13.08&longitude=80.27" 200
check "Backend rejects bad coordinates" "$BACKEND/api/v1/weather/forecast?latitude=95&longitude=80" 400
check "Admin API needs login"    "$BACKEND/api/v1/admin/alerts" 401
check "Frontend"                 "$FRONT/" 200
exit $ok
