#!/usr/bin/env bash
set -euo pipefail
source "$(dirname "$0")/_env.sh"
cd "$ROOT/backend"
exec mvn spring-boot:run
