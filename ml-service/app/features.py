"""Feature engineering shared by training AND serving (prevents train/serve skew).

Every feature for day D uses only observations from day D and earlier.
The prediction target is rainfall on day D+1 (see HORIZON_DAYS).
"""
from __future__ import annotations

import numpy as np
import pandas as pd

HORIZON_DAYS = 1
RAW_COLUMNS = [
    "precipitation_sum_mm",
    "temperature_mean_c",
    "humidity_mean_pct",
    "pressure_mean_hpa",
    "wind_max_kmh",
]
LAG_DAYS = 7                      # rain_lag0 (today) .. rain_lag6
ROLLING_WINDOWS = (3, 7, 14)
MIN_HISTORY_DAYS = max(ROLLING_WINDOWS)  # 14 consecutive days are required
RAINY_DAY_MM = 1.0                # a "rainy day" for the rainy_days_7d feature

FEATURE_COLUMNS = (
    [f"rain_lag{k}" for k in range(LAG_DAYS)]
    + [f"rain_sum_{w}d" for w in ROLLING_WINDOWS]
    + [
        "rainy_days_7d",
        "temperature_mean_c",
        "humidity_mean_pct",
        "pressure_mean_hpa",
        "pressure_change_1d_hpa",
        "wind_max_kmh",
        "doy_sin",
        "doy_cos",
        "latitude",
        "longitude",
    ]
)


def aggregate_hourly_to_daily(hourly: pd.DataFrame) -> pd.DataFrame:
    """Aggregate hourly rows (local time, naive) to local calendar days.

    Needs columns: time, precipitation, temperature_2m, relative_humidity_2m,
    surface_pressure, wind_speed_10m. A day is kept only if it has all 24 hourly
    precipitation values; otherwise its values are NaN (never silently partial).
    """
    h = hourly.copy()
    h["time"] = pd.to_datetime(h["time"])
    h["date"] = h["time"].dt.normalize()
    grp = h.groupby("date")
    n_hours = grp["precipitation"].count()
    daily = pd.DataFrame(
        {
            "precipitation_sum_mm": grp["precipitation"].sum(min_count=1),
            "temperature_mean_c": grp["temperature_2m"].mean(),
            "humidity_mean_pct": grp["relative_humidity_2m"].mean(),
            "pressure_mean_hpa": grp["surface_pressure"].mean(),
            "wind_max_kmh": grp["wind_speed_10m"].max(),
        }
    )
    daily.loc[n_hours < 24, RAW_COLUMNS] = np.nan
    return daily.reset_index()


def make_features(daily: pd.DataFrame, latitude: float, longitude: float) -> pd.DataFrame:
    """Build features for ONE location. Input needs 'date' + RAW_COLUMNS.

    Missing calendar days become NaN rows, so lags/rolling windows always refer to
    real calendar offsets (never to "the previous row").
    """
    df = daily.copy()
    df["date"] = pd.to_datetime(df["date"])
    df = df.sort_values("date").drop_duplicates("date", keep="last")
    full_index = pd.date_range(df["date"].min(), df["date"].max(), freq="D", name="date")
    df = df.set_index("date").reindex(full_index)

    rain = df["precipitation_sum_mm"]
    out = pd.DataFrame(index=df.index)
    for k in range(LAG_DAYS):
        out[f"rain_lag{k}"] = rain.shift(k)
    for w in ROLLING_WINDOWS:
        out[f"rain_sum_{w}d"] = rain.rolling(w, min_periods=w).sum()
    rainy = (rain >= RAINY_DAY_MM).astype(float).where(rain.notna())
    out["rainy_days_7d"] = rainy.rolling(7, min_periods=7).sum()
    out["temperature_mean_c"] = df["temperature_mean_c"]
    out["humidity_mean_pct"] = df["humidity_mean_pct"]
    out["pressure_mean_hpa"] = df["pressure_mean_hpa"]
    out["pressure_change_1d_hpa"] = df["pressure_mean_hpa"].diff(1)
    out["wind_max_kmh"] = df["wind_max_kmh"]
    doy = df.index.dayofyear.to_numpy()
    out["doy_sin"] = np.sin(2 * np.pi * doy / 365.25)
    out["doy_cos"] = np.cos(2 * np.pi * doy / 365.25)
    out["latitude"] = float(latitude)
    out["longitude"] = float(longitude)
    return out[FEATURE_COLUMNS].reset_index()


def build_training_frame(daily_all: pd.DataFrame) -> pd.DataFrame:
    """Features + next-day target for every location.

    daily_all columns: location, latitude, longitude, date, RAW_COLUMNS.
    Target columns: target_date, target_rain_mm (rain on D+1).
    Rows without a known target or without 14 days of history are dropped.
    """
    parts = []
    for location, g in daily_all.groupby("location", sort=True):
        lat, lon = float(g["latitude"].iloc[0]), float(g["longitude"].iloc[0])
        feats = make_features(g[["date"] + RAW_COLUMNS], lat, lon)
        feats["location"] = location
        feats["target_date"] = feats["date"] + pd.Timedelta(days=HORIZON_DAYS)
        feats["target_rain_mm"] = feats["rain_lag0"].shift(-HORIZON_DAYS)
        parts.append(feats)
    frame = pd.concat(parts, ignore_index=True)
    frame = frame.dropna(subset=["target_rain_mm", "rain_sum_14d", "rain_lag0"])
    return frame.sort_values(["date", "location"]).reset_index(drop=True)
