import json

import pandas as pd
import pytest

from app.features import build_training_frame
from tests.synth import synthetic_daily
from training.train import chronological_split, load_and_check, run_training


def test_split_is_chronological_with_gap():
    frame = build_training_frame(synthetic_daily())
    train, val, test, info = chronological_split(frame)
    assert train["date"].max() < val["date"].min() < val["date"].max() < test["date"].min()
    assert (val["date"].min() - train["date"].max()).days > info["purge_gap_days"] - 1
    assert min(len(train), len(val), len(test)) > 0


def test_training_writes_artifacts_and_metadata(trained_dir):
    meta = json.loads((trained_dir / "metadata.json").read_text())
    assert (trained_dir / "regression.joblib").exists()
    assert meta["regression"]["status"] == "trained"
    assert {"mae_mm", "rmse_mm"} <= set(meta["regression"]["test"]["baseline_train_mean"])
    assert meta["classification"]["status"] == "trained"
    assert meta["classification"]["score_is_calibrated_probability"] is False
    assert meta["split"]["train"][1] < meta["split"]["validation"][0]
    assert meta["data"]["sha256"] and meta["model_version"]


def test_too_few_heavy_days_marks_classifier_pending(tmp_path):
    csv = tmp_path / "daily.csv"
    synthetic_daily(heavy_rate=0.0).to_csv(csv, index=False)
    meta = run_training(csv, tmp_path / "m", heavy_threshold_mm=500.0)
    assert meta["classification"]["status"] == "pending_insufficient_positive_examples"
    assert not (tmp_path / "m" / "classifier.joblib").exists()
    assert (tmp_path / "m" / "regression.joblib").exists()


def test_data_quality_checks_detect_problems(tmp_path):
    df = synthetic_daily(years=1, locations=("A",))
    df = pd.concat([df, df.iloc[[5]]])                       # duplicate row
    df.loc[df.index[10], "precipitation_sum_mm"] = -3.0       # impossible value
    csv = tmp_path / "d.csv"
    df.to_csv(csv, index=False)
    _, rep = load_and_check(csv)
    assert rep["duplicate_location_dates_removed"] == 1
    assert rep["negative_precipitation_rows"] == 1


def test_missing_columns_rejected(tmp_path):
    csv = tmp_path / "bad.csv"
    pd.DataFrame({"date": ["2020-01-01"], "location": ["A"]}).to_csv(csv, index=False)
    with pytest.raises(ValueError, match="missing columns"):
        load_and_check(csv)
