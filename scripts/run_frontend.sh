#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_env.sh"
cd "$ROOT/frontend"
[ -d node_modules ] || npm install
exec npm run dev -- --host
