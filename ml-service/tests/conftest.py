import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from tests.synth import synthetic_daily  # noqa: E402
from training.train import run_training  # noqa: E402


@pytest.fixture(scope="session")
def trained_dir(tmp_path_factory):
    d = tmp_path_factory.mktemp("trained")
    csv = d / "daily.csv"
    synthetic_daily().to_csv(csv, index=False)
    run_training(csv, d / "models", heavy_threshold_mm=64.5)
    return d / "models"
