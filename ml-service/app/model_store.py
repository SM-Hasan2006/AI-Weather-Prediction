from __future__ import annotations

import json
from pathlib import Path

import joblib


class ModelStore:
    """Loads trained artefacts from disk. Never fabricates predictions."""

    def __init__(self, models_dir: Path):
        self.models_dir = Path(models_dir)
        self.metadata: dict | None = None
        self.regression = None
        self.classifier = None
        self.load_error: str | None = None

    def load(self) -> None:
        self.metadata = self.regression = self.classifier = None
        self.load_error = None
        meta_path = self.models_dir / "metadata.json"
        if not meta_path.exists():
            self.load_error = f"No trained model found in {self.models_dir} (metadata.json missing). Run the training pipeline."
            return
        try:
            self.metadata = json.loads(meta_path.read_text())
            reg = self.models_dir / "regression.joblib"
            clf = self.models_dir / "classifier.joblib"
            if reg.exists():
                self.regression = joblib.load(reg)
            if clf.exists():
                self.classifier = joblib.load(clf)
        except Exception as exc:  # corrupt files etc.
            self.metadata = self.regression = self.classifier = None
            self.load_error = f"Could not load model files: {exc}"

    @property
    def version(self) -> str | None:
        return self.metadata["model_version"] if self.metadata else None

    def info(self) -> dict:
        if not self.metadata:
            return {"available": False, "reason": self.load_error}
        m = self.metadata
        return {
            "available": True,
            "model_version": m["model_version"],
            "trained_at_utc": m["trained_at_utc"],
            "horizon_days": m["horizon_days"],
            "regression": {"loaded": self.regression is not None, "status": m["regression"]["status"],
                           "selected_model": m["regression"].get("selected_model"),
                           "test_metrics": m["regression"].get("test")},
            "classification": {"loaded": self.classifier is not None, "status": m["classification"]["status"],
                               "detail": m["classification"].get("detail"),
                               "heavy_rain_threshold_mm": m["classification"].get("heavy_rain_threshold_mm"),
                               "selected_model": m["classification"].get("selected_model"),
                               "test_metrics": m["classification"].get("test")},
            "data": {"source": (m["data"]["provenance"] or {}).get("source") if isinstance(m["data"]["provenance"], dict) else None,
                     "date_range": [m["data"]["quality_report"]["date_min"], m["data"]["quality_report"]["date_max"]],
                     "locations": m["data"]["quality_report"]["locations"]},
            "split": m["split"],
            "limitations": m["limitations"],
        }
