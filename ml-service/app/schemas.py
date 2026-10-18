from __future__ import annotations

from datetime import date, timedelta

from pydantic import BaseModel, Field, field_validator, model_validator

from app.features import MIN_HISTORY_DAYS


class DailyObservation(BaseModel):
    date: date
    precipitation_sum_mm: float = Field(ge=0, le=1500)
    temperature_mean_c: float = Field(ge=-60, le=60)
    humidity_mean_pct: float = Field(ge=0, le=100)
    pressure_mean_hpa: float = Field(ge=300, le=1100)
    wind_max_kmh: float = Field(ge=0, le=400)


class PredictRequest(BaseModel):
    latitude: float = Field(ge=-90, le=90)
    longitude: float = Field(ge=-180, le=180)
    daily: list[DailyObservation] = Field(
        description=f"Most recent daily observations, oldest first, no gaps, at least {MIN_HISTORY_DAYS} days. "
        "The last entry is 'today'; the prediction is for the following day.",
        min_length=MIN_HISTORY_DAYS, max_length=60,
    )

    @model_validator(mode="after")
    def _check_contiguous(self):
        dates = [d.date for d in self.daily]
        for prev, cur in zip(dates, dates[1:]):
            if cur - prev != timedelta(days=1):
                raise ValueError("daily observations must be consecutive calendar days in ascending order with no gaps")
        return self


class Prediction(BaseModel):
    target_definition: str
    forecast_horizon_days: int
    as_of_date: date
    target_date: date
    model_version: str
    limitations: list[str]


class RainfallPrediction(Prediction):
    predicted_rainfall_mm: float


class HeavyRainPrediction(Prediction):
    heavy_rain_threshold_mm: float
    predicted_heavy_rain: bool
    score: float
    score_is_calibrated_probability: bool
    decision_threshold: float
    note: str


class ErrorResponse(BaseModel):
    code: str
    message: str
