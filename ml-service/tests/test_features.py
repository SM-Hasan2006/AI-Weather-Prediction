import numpy as np
import pandas as pd

from app.features import FEATURE_COLUMNS, RAW_COLUMNS, aggregate_hourly_to_daily, build_training_frame, make_features
from tests.synth import synthetic_daily


def _one_location(n=40):
    return synthetic_daily(years=1, locations=("A",)).iloc[:n][["date"] + RAW_COLUMNS].reset_index(drop=True)


def test_lag_matches_calendar_offset():
    d = _one_location()
    f = make_features(d, 10, 78).set_index("date")
    day = d["date"].iloc[20]
    assert f.loc[day, "rain_lag0"] == d["precipitation_sum_mm"].iloc[20]
    assert f.loc[day, "rain_lag3"] == d["precipitation_sum_mm"].iloc[17]
    assert np.isclose(f.loc[day, "rain_sum_3d"], d["precipitation_sum_mm"].iloc[18:21].sum())


def test_no_future_leakage_into_features():
    d = _one_location()
    changed = d.copy()
    changed.loc[25:, "precipitation_sum_mm"] = 999.0  # alter the "future"
    a = make_features(d, 10, 78).set_index("date")
    b = make_features(changed, 10, 78).set_index("date")
    cutoff = d["date"].iloc[24]
    pd.testing.assert_frame_equal(a.loc[:cutoff], b.loc[:cutoff])


def test_missing_day_produces_nan_not_shifted_value():
    d = _one_location().drop(index=10).reset_index(drop=True)  # remove one calendar day
    f = make_features(d, 10, 78).set_index("date")
    missing_day = _one_location()["date"].iloc[10]
    assert np.isnan(f.loc[missing_day, "rain_lag0"])
    assert np.isnan(f.loc[missing_day + pd.Timedelta(days=1), "rain_lag1"])


def test_target_is_next_day_rain():
    daily = synthetic_daily(years=1, locations=("A",))
    frame = build_training_frame(daily).set_index("date")
    day = frame.index[30]
    nxt = daily.set_index("date").loc[day + pd.Timedelta(days=1), "precipitation_sum_mm"]
    assert frame.loc[day, "target_rain_mm"] == nxt
    assert list(frame[FEATURE_COLUMNS].columns) == FEATURE_COLUMNS


def test_incomplete_hourly_day_becomes_nan():
    times = pd.date_range("2024-01-01", periods=24 + 10, freq="h")  # day 2 has only 10 hours
    h = pd.DataFrame({"time": times, "precipitation": 1.0, "temperature_2m": 25.0,
                      "relative_humidity_2m": 60.0, "surface_pressure": 1000.0, "wind_speed_10m": 10.0})
    daily = aggregate_hourly_to_daily(h).set_index("date")
    assert daily.iloc[0]["precipitation_sum_mm"] == 24.0
    assert np.isnan(daily.iloc[1]["precipitation_sum_mm"])
