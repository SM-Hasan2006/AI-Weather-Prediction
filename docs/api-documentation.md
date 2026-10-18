# API documentation

Base URL: `http://localhost:8080/api/v1`. Interactive docs once the backend runs: `http://localhost:8080/swagger-ui.html`.
Coordinates are rounded to 2 decimals (~1 km). Errors share one shape:

```json
{ "timestamp": "...", "status": 503, "code": "weather_provider_unavailable", "message": "…", "details": [] }
```

| Method & path | Auth | Purpose |
|---|---|---|
| `GET /health` | – | `status` (UP / DEGRADED when only the ML service is down / DOWN when the DB is down), `database`, `mlService` |
| `GET /locations/search?q=` | – | Geocoding (Open-Meteo). `q` 2–80 chars |
| `GET /weather/forecast?latitude&longitude` | – | Current + 7 daily + hourly, `provider`, `fetchedAt`, `forecastStart/End`, `stale` |
| `GET /weather/current` · `/weather/hourly` | – | Views of the same cached forecast |
| `GET /weather/history?latitude&longitude&days=30` | – | Archive rainfall (max 92 days) + earlier forecasts saved by this system, where they exist |
| `GET /risk?latitude&longitude` | – | Rule-based category, reasons, per-day categories, thresholds used, method + version, disclaimer |
| `GET /alerts?latitude&longitude` | – | `official` (administrator-entered), `systemGenerated` (screening estimates), official links |
| `GET /ml/status` | – | Whether the ML model is loaded; metadata and held-out metrics if so |
| `POST /ml/predict?latitude&longitude` | – | Backend loads ≥14 complete recent days, calls the ML service, returns rainfall estimate and heavy-rain flag, or explanatory `messages` |
| `GET /admin/alerts` | Admin | List all alerts |
| `POST /admin/alerts` | Admin | Create (201). Duplicate (same source, areas, period) → 409 |
| `PUT /admin/alerts/{id}` | Admin | Update |
| `DELETE /admin/alerts/{id}` | Admin | Delete (204) |

Status codes: 400 invalid input, 401 admin auth missing/wrong, 404 unknown id, 409 duplicate alert, 429 rate limit,
503 provider unavailable, 500 unexpected (generic message; details only in the server log).

Alert request body (admin): `title, message, severity (INFO|YELLOW|ORANGE|RED), sourceName, sourceUrl (https only),
issuedAt, validFrom, validTo (ISO-8601 instants), affectedAreas, [centerLatitude, centerLongitude, radiusKm], isTest`.

## ML service (`http://localhost:8000`, docs at `/docs`)

| Endpoint | Purpose |
|---|---|
| `GET /health` | Service and model availability |
| `GET /model/info` | Version, data coverage, split dates, metrics, limitations (or `available:false` + reason) |
| `POST /predict/rainfall` | `predicted_rainfall_mm` for the day after the last supplied day |
| `POST /predict/heavy-rain` | `predicted_heavy_rain`, raw `score`, `score_is_calibrated_probability`, threshold used |

Request: `{latitude, longitude, daily:[{date, precipitation_sum_mm, temperature_mean_c, humidity_mean_pct, pressure_mean_hpa, wind_max_kmh}×14..60]}`,
oldest first, consecutive days, no gaps. Invalid input → 422 `invalid_input`; model missing → 503 `model_unavailable`;
optional `X-ML-Api-Key` header when `ML_SERVICE_API_KEY` is set.
