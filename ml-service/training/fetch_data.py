"""Download REAL historical weather from the Open-Meteo Historical Weather API.

Usage (from ml-service/):
    python -m training.fetch_data --start 2010-01-01 --end 2024-12-31

Source: https://open-meteo.com/en/docs/historical-weather-api
Endpoint: https://archive-api.open-meteo.com/v1/archive
NOTE: this is a *reanalysis* product (gridded model + assimilated observations),
not raw rain-gauge data. Grid values smooth out local extremes. See docs.
Open-Meteo's free API is for non-commercial use and requires attribution (CC BY 4.0).
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
import time
from datetime import date, datetime, timezone
from pathlib import Path

import pandas as pd
import requests
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry

from app.features import aggregate_hourly_to_daily

API_URL = "https://archive-api.open-meteo.com/v1/archive"
HOURLY_VARS = ["temperature_2m", "relative_humidity_2m", "surface_pressure", "wind_speed_10m", "precipitation"]
UNITS = {
    "temperature_2m": "°C",
    "relative_humidity_2m": "%",
    "surface_pressure": "hPa",
    "wind_speed_10m": "km/h",
    "precipitation": "mm (per hour)",
}
ROOT = Path(__file__).resolve().parent.parent
DEFAULT_LOCATIONS = Path(__file__).with_name("locations.json")


def make_session() -> requests.Session:
    s = requests.Session()
    retry = Retry(total=4, backoff_factor=2, status_forcelist=(429, 500, 502, 503, 504), allowed_methods=("GET",))
    s.mount("https://", HTTPAdapter(max_retries=retry))
    return s


def fetch_year(session, lat, lon, start: date, end: date, timezone_name: str) -> pd.DataFrame:
    params = {
        "latitude": lat,
        "longitude": lon,
        "start_date": start.isoformat(),
        "end_date": end.isoformat(),
        "hourly": ",".join(HOURLY_VARS),
        "timezone": timezone_name,
        "wind_speed_unit": "kmh",
        "temperature_unit": "celsius",
        "precipitation_unit": "mm",
    }
    r = session.get(API_URL, params=params, timeout=(10, 60))
    if r.status_code != 200:
        raise RuntimeError(f"Open-Meteo returned HTTP {r.status_code}: {r.text[:300]}")
    payload = r.json()
    if "hourly" not in payload:
        raise RuntimeError(f"Unexpected Open-Meteo response (no 'hourly'): {str(payload)[:300]}")
    return pd.DataFrame(payload["hourly"])


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--start", default="2010-01-01")
    ap.add_argument("--end", default="2024-12-31")
    ap.add_argument("--locations", default=str(DEFAULT_LOCATIONS))
    ap.add_argument("--timezone", default="Asia/Kolkata")
    ap.add_argument("--out", default=str(ROOT / "data" / "processed" / "daily.csv"))
    ap.add_argument("--pause", type=float, default=1.0, help="seconds to wait between API calls")
    args = ap.parse_args(argv)

    locations = json.loads(Path(args.locations).read_text())
    start, end = date.fromisoformat(args.start), date.fromisoformat(args.end)
    session = make_session()
    daily_parts = []
    for loc in locations:
        yearly = []
        for year in range(start.year, end.year + 1):
            y0, y1 = max(start, date(year, 1, 1)), min(end, date(year, 12, 31))
            print(f"{loc['name']}: {y0} .. {y1}", flush=True)
            try:
                yearly.append(fetch_year(session, loc["latitude"], loc["longitude"], y0, y1, args.timezone))
            except Exception as exc:  # fail loudly - never fabricate data
                print(f"ERROR: could not download {loc['name']} {year}: {exc}", file=sys.stderr)
                return 1
            time.sleep(args.pause)
        daily = aggregate_hourly_to_daily(pd.concat(yearly, ignore_index=True))
        daily.insert(0, "location", loc["name"])
        daily.insert(1, "latitude", loc["latitude"])
        daily.insert(2, "longitude", loc["longitude"])
        daily_parts.append(daily)

    all_daily = pd.concat(daily_parts, ignore_index=True)
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    all_daily.to_csv(out, index=False)
    digest = hashlib.sha256(out.read_bytes()).hexdigest()
    provenance = {
        "source": "Open-Meteo Historical Weather API (reanalysis)",
        "source_url": API_URL,
        "documentation_url": "https://open-meteo.com/en/docs/historical-weather-api",
        "license_note": "CC BY 4.0 attribution required; free API is for non-commercial use. Verify current terms at https://open-meteo.com/en/terms",
        "collected_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "requested_period": {"start": args.start, "end": args.end},
        "timezone": args.timezone,
        "hourly_variables_and_units": UNITS,
        "daily_aggregation": "local calendar day; precipitation=sum, temperature/humidity/pressure=mean, wind=max; days with <24 hourly values set to NaN",
        "locations": locations,
        "rows": int(len(all_daily)),
        "sha256": digest,
    }
    (out.parent / "provenance.json").write_text(json.dumps(provenance, indent=2, ensure_ascii=False))
    print(f"Wrote {out} ({len(all_daily)} rows) and provenance.json")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
