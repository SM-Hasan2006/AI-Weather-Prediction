from __future__ import annotations

import pandas as pd
from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.config import Settings
from app.features import HORIZON_DAYS, RAW_COLUMNS, make_features
from app.model_store import ModelStore
from app.schemas import HeavyRainPrediction, PredictRequest, RainfallPrediction

APP_VERSION = "1.0.0"


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings.from_env()
    app = FastAPI(title="AI Weather ML Service", version=APP_VERSION,
                  description="Next-day rainfall and heavy-rain estimates from past observations. Not an official warning.")
    store = ModelStore(settings.models_dir)
    store.load()
    app.state.store = store

    app.add_middleware(CORSMiddleware, allow_origins=settings.cors_allowed_origins,
                       allow_methods=["GET", "POST"], allow_headers=["Content-Type", "X-ML-Api-Key"])

    def require_key(x_ml_api_key: str | None = Header(default=None)):
        if settings.api_key and x_ml_api_key != settings.api_key:
            raise HTTPException(status_code=401, detail={"code": "unauthorized", "message": "Missing or invalid API key."})

    def _features(req: PredictRequest, columns: list[str]) -> tuple[pd.DataFrame, pd.Timestamp]:
        df = pd.DataFrame([d.model_dump() for d in req.daily])
        feats = make_features(df[["date"] + RAW_COLUMNS], req.latitude, req.longitude)
        row = feats.iloc[[-1]]
        return row[columns], pd.Timestamp(row["date"].iloc[0])

    def _common(as_of: pd.Timestamp) -> dict:
        return dict(forecast_horizon_days=HORIZON_DAYS, as_of_date=as_of.date(),
                    target_date=(as_of + pd.Timedelta(days=HORIZON_DAYS)).date(),
                    model_version=store.version, limitations=store.metadata["limitations"])

    @app.exception_handler(RequestValidationError)
    async def _validation(_: Request, exc: RequestValidationError):
        errs = [{"field": ".".join(str(p) for p in e["loc"] if p != "body"), "message": e["msg"]} for e in exc.errors()]
        return JSONResponse(status_code=422, content={"code": "invalid_input", "message": "Request validation failed.", "errors": errs})

    @app.exception_handler(HTTPException)
    async def _http(_: Request, exc: HTTPException):
        detail = exc.detail if isinstance(exc.detail, dict) else {"code": "error", "message": str(exc.detail)}
        return JSONResponse(status_code=exc.status_code, content=detail)

    @app.get("/health")
    def health():
        return {"status": "ok", "service": "ml-service", "version": APP_VERSION,
                "model_available": store.metadata is not None, "model_version": store.version}

    @app.get("/model/info", dependencies=[Depends(require_key)])
    def model_info():
        return store.info()

    def _need(model, name: str):
        if model is None:
            reason = store.load_error or f"The {name} model is not available (not trained or training was marked insufficient-data)."
            raise HTTPException(status_code=503, detail={"code": "model_unavailable", "message": reason})

    @app.post("/predict/rainfall", response_model=RainfallPrediction, dependencies=[Depends(require_key)])
    def predict_rainfall(req: PredictRequest):
        _need(store.regression, "rainfall regression")
        X, as_of = _features(req, store.regression["feature_columns"])
        value = max(0.0, float(store.regression["pipeline"].predict(X)[0]))
        return RainfallPrediction(
            target_definition=store.metadata["regression"]["target"],
            predicted_rainfall_mm=round(value, 2), **_common(as_of))

    @app.post("/predict/heavy-rain", response_model=HeavyRainPrediction, dependencies=[Depends(require_key)])
    def predict_heavy(req: PredictRequest):
        _need(store.classifier, "heavy-rain classifier")
        X, as_of = _features(req, store.classifier["feature_columns"])
        score = float(store.classifier["pipeline"].predict_proba(X)[0, 1])
        cfg = store.metadata["classification"]
        return HeavyRainPrediction(
            target_definition=cfg["target"], heavy_rain_threshold_mm=cfg["heavy_rain_threshold_mm"],
            predicted_heavy_rain=score >= store.classifier["threshold"], score=round(score, 4),
            score_is_calibrated_probability=bool(cfg["score_is_calibrated_probability"]),
            decision_threshold=round(float(store.classifier["threshold"]), 4),
            note="The score ranks risk; it is NOT a validated probability. This is an estimate from past observations, not an official warning or flood prediction.",
            **_common(as_of))

    return app


app = create_app()
