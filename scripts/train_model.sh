#!/usr/bin/env bash
# Downloads REAL historical data from Open-Meteo and trains the models. Needs internet access.
# Usage: bash scripts/train_model.sh [START_DATE] [END_DATE]
set -euo pipefail
cd "$(dirname "$0")/../ml-service"
[ -d .venv ] || { python3 -m venv .venv && .venv/bin/pip install -r requirements.txt; }
. .venv/bin/activate
python -m training.fetch_data --start "${1:-2010-01-01}" --end "${2:-2024-12-31}"
python -m training.train --heavy-threshold-mm "${HEAVY_THRESHOLD_MM:-64.5}"
echo "Done. See ml-service/models/metadata.json for the real evaluation results."
