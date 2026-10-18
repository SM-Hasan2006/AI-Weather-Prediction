#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_env.sh"
cd "$ROOT/ml-service"
[ -d .venv ] || { python3 -m venv .venv && .venv/bin/pip install -r requirements.txt; }
. .venv/bin/activate
exec uvicorn app.main:app --host 0.0.0.0 --port "${ML_PORT:-8000}"
