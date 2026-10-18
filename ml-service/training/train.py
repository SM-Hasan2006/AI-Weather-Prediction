"""Train and evaluate rainfall models on REAL historical data (output of fetch_data).

    python -m training.train --data data/processed/daily.csv --heavy-threshold-mm 64.5

Task A (regression):     rainfall (mm) on day D+1.
Task B (classification): will rainfall on day D+1 be >= heavy threshold?

Rules enforced here:
* chronological train / validation / test split with a purge gap (no random split)
* imputer / scaler are inside the sklearn Pipeline -> fitted on training rows only
* models are selected on VALIDATION; the TEST period is only used for the final report
* if there are too few heavy-rain examples, the classifier is NOT trained and the
  report says so (no fabricated results)
"""
from __future__ import annotations

import argparse
import hashlib
import json
import platform
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
import sklearn
from sklearn.dummy import DummyRegressor
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    brier_score_loss,
    confusion_matrix,
    f1_score,
    mean_absolute_error,
    mean_squared_error,
    precision_recall_curve,
    precision_score,
    recall_score,
)
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from app.features import FEATURE_COLUMNS, HORIZON_DAYS, RAW_COLUMNS, build_training_frame

ROOT = Path(__file__).resolve().parent.parent
RANDOM_STATE = 42


# ----------------------------------------------------------------------------- data
def load_and_check(csv_path: Path) -> tuple[pd.DataFrame, dict]:
    df = pd.read_csv(csv_path, parse_dates=["date"])
    required = {"location", "latitude", "longitude", "date", *RAW_COLUMNS}
    missing = required - set(df.columns)
    if missing:
        raise ValueError(f"Data file is missing columns: {sorted(missing)}")
    report: dict = {"rows_raw": int(len(df))}
    dup = int(df.duplicated(["location", "date"]).sum())
    report["duplicate_location_dates_removed"] = dup
    df = df.drop_duplicates(["location", "date"], keep="last")
    report["missing_values_per_column"] = {c: int(df[c].isna().sum()) for c in RAW_COLUMNS}
    report["negative_precipitation_rows"] = int((df["precipitation_sum_mm"] < 0).sum())
    df.loc[df["precipitation_sum_mm"] < 0, "precipitation_sum_mm"] = np.nan
    report["humidity_out_of_0_100"] = int(((df["humidity_mean_pct"] < 0) | (df["humidity_mean_pct"] > 100)).sum())
    gaps = {}
    for loc, g in df.groupby("location"):
        expected = (g["date"].max() - g["date"].min()).days + 1
        gaps[loc] = int(expected - g["date"].nunique())
    report["missing_calendar_days_per_location"] = gaps
    report["date_min"] = str(df["date"].min().date())
    report["date_max"] = str(df["date"].max().date())
    report["locations"] = sorted(df["location"].unique().tolist())
    return df, report


def chronological_split(frame: pd.DataFrame, train_frac=0.70, val_frac=0.15, gap_days=HORIZON_DAYS):
    """Split by calendar date (same cut dates for every location).

    gap_days days are purged between splits so the label of the last training
    row (rain on D+1) cannot appear as a feature in the first validation row.
    """
    dates = np.sort(frame["date"].unique())
    t_end = dates[int(len(dates) * train_frac) - 1]
    v_end = dates[int(len(dates) * (train_frac + val_frac)) - 1]
    gap = np.timedelta64(gap_days, "D")
    train = frame[frame["date"] <= t_end]
    val = frame[(frame["date"] > t_end + gap) & (frame["date"] <= v_end)]
    test = frame[frame["date"] > v_end + gap]
    info = {
        "train": [str(pd.Timestamp(train["date"].min()).date()), str(pd.Timestamp(t_end).date())],
        "validation": [str(pd.Timestamp(val["date"].min()).date()), str(pd.Timestamp(v_end).date())],
        "test": [str(pd.Timestamp(test["date"].min()).date()), str(pd.Timestamp(test["date"].max()).date())],
        "purge_gap_days": gap_days,
        "rows": {"train": int(len(train)), "validation": int(len(val)), "test": int(len(test))},
    }
    return train, val, test, info


# ------------------------------------------------------------------------- metrics
def regression_metrics(y_true, y_pred) -> dict:
    return {
        "mae_mm": float(mean_absolute_error(y_true, y_pred)),
        "rmse_mm": float(np.sqrt(mean_squared_error(y_true, y_pred))),
    }


def calibration_bins(y_true, score, n_bins=10) -> list[dict]:
    y_true, score = np.asarray(y_true), np.asarray(score)
    edges = np.linspace(0, 1, n_bins + 1)
    idx = np.clip(np.digitize(score, edges[1:-1]), 0, n_bins - 1)
    out = []
    for b in range(n_bins):
        m = idx == b
        if m.any():
            out.append({"bin": f"{edges[b]:.1f}-{edges[b+1]:.1f}", "n": int(m.sum()),
                        "mean_score": float(score[m].mean()), "observed_rate": float(y_true[m].mean())})
    return out


def classification_metrics(y_true, score, threshold) -> dict:
    y_true = np.asarray(y_true).astype(int)
    pred = (np.asarray(score) >= threshold).astype(int)
    tn, fp, fn, tp = confusion_matrix(y_true, pred, labels=[0, 1]).ravel()
    return {
        "threshold": float(threshold),
        "precision": float(precision_score(y_true, pred, zero_division=0)),
        "recall": float(recall_score(y_true, pred, zero_division=0)),
        "f1": float(f1_score(y_true, pred, zero_division=0)),
        "pr_auc": float(average_precision_score(y_true, score)),
        "brier": float(brier_score_loss(y_true, np.clip(score, 0, 1))),
        "confusion_matrix": {"tn": int(tn), "fp": int(fp), "fn": int(fn), "tp": int(tp)},
        "positives": int(y_true.sum()),
        "n": int(len(y_true)),
        "calibration_bins": calibration_bins(y_true, score),
    }


def best_f1_threshold(y_true, score) -> float:
    p, r, t = precision_recall_curve(y_true, score)
    f1 = 2 * p[:-1] * r[:-1] / np.clip(p[:-1] + r[:-1], 1e-12, None)
    return float(t[int(np.nanargmax(f1))])


# --------------------------------------------------------------------------- tasks
def _pipe(model, scale=False) -> Pipeline:
    steps = [("imputer", SimpleImputer(strategy="median"))]
    if scale:
        steps.append(("scaler", StandardScaler()))
    steps.append(("model", model))
    return Pipeline(steps)


def train_regression(train, val, test) -> tuple[dict, Pipeline | None]:
    X = lambda d: d[FEATURE_COLUMNS]
    y = lambda d: d["target_rain_mm"].to_numpy()
    candidates = {
        "random_forest_regressor": _pipe(RandomForestRegressor(
            n_estimators=300, min_samples_leaf=3, n_jobs=-1, random_state=RANDOM_STATE)),
    }
    baselines = {
        "baseline_persistence (tomorrow = today)": lambda d: d["rain_lag0"].to_numpy(),
        "baseline_train_mean": None,
    }
    train_mean = DummyRegressor(strategy="mean").fit(X(train), y(train))
    val_results, fitted = {}, {}
    for name, pipe in candidates.items():
        pipe.fit(X(train), y(train))
        fitted[name] = pipe
        val_results[name] = regression_metrics(y(val), pipe.predict(X(val)))
    val_results["baseline_persistence (tomorrow = today)"] = regression_metrics(y(val), baselines["baseline_persistence (tomorrow = today)"](val))
    val_results["baseline_train_mean"] = regression_metrics(y(val), train_mean.predict(X(val)))
    best = min(candidates, key=lambda n: val_results[n]["mae_mm"])
    test_results = {
        best: regression_metrics(y(test), fitted[best].predict(X(test))),
        "baseline_persistence (tomorrow = today)": regression_metrics(y(test), baselines["baseline_persistence (tomorrow = today)"](test)),
        "baseline_train_mean": regression_metrics(y(test), train_mean.predict(X(test))),
    }
    rf = fitted[best].named_steps["model"]
    importances = sorted(zip(FEATURE_COLUMNS, rf.feature_importances_), key=lambda t: -t[1])[:8]
    beats = test_results[best]["mae_mm"] < min(v["mae_mm"] for k, v in test_results.items() if k != best)
    return {
        "status": "trained",
        "target": f"rainfall_mm on day D+{HORIZON_DAYS}",
        "selected_model": best,
        "selection_metric": "validation MAE",
        "validation": val_results,
        "test": test_results,
        "beats_all_baselines_on_test_mae": bool(beats),
        "top_feature_importances": [{"feature": f, "importance": float(i)} for f, i in importances],
    }, fitted[best]


def train_classification(train, val, test, heavy_mm, min_pos) -> tuple[dict, dict | None]:
    X = lambda d: d[FEATURE_COLUMNS]
    lab = lambda d: (d["target_rain_mm"] >= heavy_mm).astype(int).to_numpy()
    ytr, yva, yte = lab(train), lab(val), lab(test)
    counts = {"train": int(ytr.sum()), "validation": int(yva.sum()), "test": int(yte.sum())}
    need = {"train": min_pos, "validation": max(5, min_pos // 6), "test": max(5, min_pos // 6)}
    short = {k: (counts[k], need[k]) for k in counts if counts[k] < need[k]}
    base = {"heavy_rain_threshold_mm": heavy_mm, "positive_counts": counts, "min_positives_required": need}
    if short:
        return {**base, "status": "pending_insufficient_positive_examples",
                "detail": f"Too few heavy-rain days (found, needed): {short}. No classifier was trained. "
                          "Use more years/locations, a lower documented threshold, or station data."}, None

    candidates = {
        "logistic_regression": _pipe(LogisticRegression(class_weight="balanced", max_iter=2000), scale=True),
        "random_forest_classifier": _pipe(RandomForestClassifier(
            n_estimators=300, min_samples_leaf=5, class_weight="balanced_subsample", n_jobs=-1, random_state=RANDOM_STATE)),
    }
    val_results, fitted, thresholds = {}, {}, {}
    for name, pipe in candidates.items():
        pipe.fit(X(train), ytr)
        fitted[name] = pipe
        s = pipe.predict_proba(X(val))[:, 1]
        thresholds[name] = best_f1_threshold(yva, s)
        val_results[name] = classification_metrics(yva, s, thresholds[name])
    best = max(candidates, key=lambda n: val_results[n]["pr_auc"])
    s_test = fitted[best].predict_proba(X(test))[:, 1]
    persistence = (test["rain_lag0"].to_numpy() >= heavy_mm).astype(float)
    test_results = {
        best: classification_metrics(yte, s_test, thresholds[best]),
        "baseline_persistence (heavy today -> heavy tomorrow)": classification_metrics(yte, persistence, 0.5),
        "baseline_never_heavy": classification_metrics(yte, np.zeros(len(yte)), 0.5),
    }
    return {
        **base,
        "status": "trained",
        "target": f"rainfall on day D+{HORIZON_DAYS} >= {heavy_mm} mm",
        "selected_model": best,
        "selection_metric": "validation PR-AUC",
        "decision_threshold": thresholds[best],
        "decision_threshold_chosen_on": "validation (max F1)",
        "score_is_calibrated_probability": False,
        "class_imbalance_handling": "class_weight balanced; PR-AUC used for selection",
        "validation": val_results,
        "test": test_results,
        "false_negative_note": "A false negative is a heavy-rain day the model missed; it is the costlier error for warnings, so recall is reported next to precision.",
    }, {"pipeline": fitted[best], "threshold": thresholds[best]}


# ---------------------------------------------------------------------------- main
def run_training(data_csv: Path, out_dir: Path, heavy_threshold_mm: float = 64.5,
                 min_train_positives: int = 30, train_frac=0.70, val_frac=0.15) -> dict:
    daily, quality = load_and_check(Path(data_csv))
    frame = build_training_frame(daily)
    train, val, test, split = chronological_split(frame, train_frac, val_frac)
    if min(len(train), len(val), len(test)) == 0:
        raise ValueError("A split is empty - not enough data for chronological evaluation.")

    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    reg_report, reg_pipe = train_regression(train, val, test)
    clf_report, clf_bundle = train_classification(train, val, test, heavy_threshold_mm, min_train_positives)

    data_hash = hashlib.sha256(Path(data_csv).read_bytes()).hexdigest()
    version = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S") + "-" + data_hash[:8]
    joblib.dump({"pipeline": reg_pipe, "feature_columns": FEATURE_COLUMNS}, out_dir / "regression.joblib")
    if clf_bundle:
        joblib.dump({**clf_bundle, "feature_columns": FEATURE_COLUMNS}, out_dir / "classifier.joblib")
    elif (out_dir / "classifier.joblib").exists():
        (out_dir / "classifier.joblib").unlink()  # never leave a stale classifier behind

    prov_path = Path(data_csv).with_name("provenance.json")
    metadata = {
        "model_version": version,
        "trained_at_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "environment": {"python": platform.python_version(), "scikit_learn": sklearn.__version__},
        "horizon_days": HORIZON_DAYS,
        "feature_columns": FEATURE_COLUMNS,
        "data": {
            "file": str(Path(data_csv).name), "sha256": data_hash, "quality_report": quality,
            "provenance": json.loads(prov_path.read_text()) if prov_path.exists() else "provenance.json not found",
        },
        "split": split,
        "regression": reg_report,
        "classification": clf_report,
        "limitations": [
            "Trained on reanalysis grid data, not rain-gauge observations; grid values under-represent local extremes.",
            "Predicts only 1 day ahead from past observations; it does not use numerical weather forecasts.",
            "A rainfall estimate is not a flood prediction.",
        ],
    }
    (out_dir / "metadata.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False))
    return metadata


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--data", default=str(ROOT / "data" / "processed" / "daily.csv"))
    ap.add_argument("--out", default=str(ROOT / "models"))
    ap.add_argument("--heavy-threshold-mm", type=float, default=64.5,
                    help="24h rainfall defining 'heavy rain' (default 64.5 mm, the lower bound of IMD's 'heavy rainfall' class; verify against current IMD documents)")
    ap.add_argument("--min-train-positives", type=int, default=30)
    args = ap.parse_args(argv)
    if not Path(args.data).exists():
        print(f"ERROR: {args.data} not found. Run:  python -m training.fetch_data", file=sys.stderr)
        return 1
    meta = run_training(Path(args.data), Path(args.out), args.heavy_threshold_mm, args.min_train_positives)
    print(json.dumps({k: meta[k] for k in ("model_version", "split")}, indent=2))
    print("Regression test metrics:", json.dumps(meta["regression"]["test"], indent=2))
    print("Classification status:", meta["classification"]["status"])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
