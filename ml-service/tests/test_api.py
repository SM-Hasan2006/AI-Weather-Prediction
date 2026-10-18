from datetime import date, timedelta

import pytest
from fastapi.testclient import TestClient

from app.config import Settings
from app.main import create_app


def payload(days=20, **over):
    start = date(2024, 6, 1)
    daily = [{"date": (start + timedelta(days=i)).isoformat(), "precipitation_sum_mm": 2.0 + i % 3,
              "temperature_mean_c": 29.0, "humidity_mean_pct": 75.0, "pressure_mean_hpa": 1004.0, "wind_max_kmh": 18.0}
             for i in range(days)]
    body = {"latitude": 11.0, "longitude": 78.0, "daily": daily}
    body.update(over)
    return body


@pytest.fixture()
def client(trained_dir):
    return TestClient(create_app(Settings(models_dir=trained_dir, cors_allowed_origins=["http://localhost:5173"])))


@pytest.fixture()
def empty_client(tmp_path):
    return TestClient(create_app(Settings(models_dir=tmp_path)))


def test_health(client):
    r = client.get("/health")
    assert r.status_code == 200 and r.json()["model_available"] is True


def test_model_info(client):
    j = client.get("/model/info").json()
    assert j["available"] and j["horizon_days"] == 1 and j["classification"]["loaded"]


def test_predict_rainfall_valid(client):
    r = client.post("/predict/rainfall", json=payload())
    assert r.status_code == 200, r.text
    j = r.json()
    assert j["predicted_rainfall_mm"] >= 0 and j["forecast_horizon_days"] == 1
    assert j["target_date"] == "2024-06-21" and j["model_version"] and j["limitations"]


def test_predict_heavy_rain_valid(client):
    j = client.post("/predict/heavy-rain", json=payload()).json()
    assert 0 <= j["score"] <= 1 and isinstance(j["predicted_heavy_rain"], bool)
    assert j["score_is_calibrated_probability"] is False and "not an official warning" in j["note"]


@pytest.mark.parametrize("body", [
    payload(days=13),                                    # too little history
    payload(latitude=123),                               # bad coordinate
    payload(daily=[]),                                   # empty
])
def test_invalid_inputs_rejected(client, body):
    r = client.post("/predict/rainfall", json=body)
    assert r.status_code == 422 and r.json()["code"] == "invalid_input"


def test_gap_in_dates_rejected(client):
    b = payload()
    b["daily"].pop(5)
    assert client.post("/predict/rainfall", json=b).status_code == 422


def test_impossible_values_rejected(client):
    for field, value in [("precipitation_sum_mm", -1), ("humidity_mean_pct", 140), ("pressure_mean_hpa", 5)]:
        b = payload()
        b["daily"][3][field] = value
        assert client.post("/predict/heavy-rain", json=b).status_code == 422


def test_missing_model_files(empty_client):
    assert empty_client.get("/health").json()["model_available"] is False
    assert empty_client.get("/model/info").json()["available"] is False
    r = empty_client.post("/predict/rainfall", json=payload())
    assert r.status_code == 503 and r.json()["code"] == "model_unavailable"


def test_api_key_enforced_when_configured(trained_dir):
    c = TestClient(create_app(Settings(models_dir=trained_dir, api_key="secret")))
    assert c.post("/predict/rainfall", json=payload()).status_code == 401
    assert c.post("/predict/rainfall", json=payload(), headers={"X-ML-Api-Key": "secret"}).status_code == 200


def test_cors_only_for_configured_origin(client):
    ok = client.get("/health", headers={"Origin": "http://localhost:5173"})
    bad = client.get("/health", headers={"Origin": "http://evil.example"})
    assert ok.headers.get("access-control-allow-origin") == "http://localhost:5173"
    assert "access-control-allow-origin" not in bad.headers
