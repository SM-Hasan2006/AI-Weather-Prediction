"""SYNTHETIC data used ONLY by automated tests to exercise the pipeline mechanics.
Nothing produced from this data is ever reported as model accuracy."""
import numpy as np
import pandas as pd

from app.features import RAW_COLUMNS


def synthetic_daily(years=6, locations=("A", "B", "C"), seed=0, heavy_rate=0.02):
    rng = np.random.default_rng(seed)
    dates = pd.date_range("2012-01-01", periods=365 * years, freq="D")
    parts = []
    for i, loc in enumerate(locations):
        doy = dates.dayofyear.to_numpy()
        wet = rng.random(len(dates)) < (0.25 + 0.2 * np.sin(2 * np.pi * doy / 365))
        rain = np.where(wet, rng.gamma(1.2, 6.0, len(dates)), 0.0)
        heavy = rng.random(len(dates)) < heavy_rate
        rain = np.where(heavy, rng.uniform(65, 150, len(dates)), rain)
        parts.append(pd.DataFrame({
            "location": loc, "latitude": 10.0 + i, "longitude": 78.0 + i, "date": dates,
            "precipitation_sum_mm": rain.round(1),
            "temperature_mean_c": 28 + 3 * np.sin(2 * np.pi * doy / 365) + rng.normal(0, 1, len(dates)),
            "humidity_mean_pct": np.clip(70 + 15 * wet + rng.normal(0, 5, len(dates)), 20, 100),
            "pressure_mean_hpa": 1005 + rng.normal(0, 3, len(dates)),
            "wind_max_kmh": np.abs(rng.normal(20, 6, len(dates))),
        }))
    return pd.concat(parts, ignore_index=True)[["location", "latitude", "longitude", "date", *RAW_COLUMNS]]
