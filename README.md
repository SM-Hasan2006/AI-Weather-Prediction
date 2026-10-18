# AI Weather & Heavy Rain Prediction System
*Intelligent Rainfall Forecasting, Heavy-Rain Risk Assessment and Weather Alert Platform*

A decision-support web application for Tamil Nadu (extensible to other regions). It is **not** an official disaster-warning service.

## Project status (honest summary)

| Part | Status |
|---|---|
| React frontend | Built, **29 tests pass**, production build succeeds. Not yet seen against a live backend. |
| Python ML service + training pipeline | Built, **22 tests pass**, smoke-tested. **No model is trained yet** – it needs real data downloaded from Open-Meteo (`scripts/train_model.sh`). No accuracy is claimed anywhere. |
| MySQL schema (Flyway) | **Verified on MySQL 8.0** (tables, constraints, cascades). |
| Spring Boot backend | Fully written (44 Java files, parses with `javac`) but **not compiled or tested** in the environment that generated it (no access to Maven Central). Expect to fix small compile errors on your first `mvn test`. |
| Real Open-Meteo calls | Implemented, never executed there (no network access to the provider). |
| Deployment | Documented, not executed. |

Details: [`docs/testing-report.md`](docs/testing-report.md).

## Overview, problem and objectives
People see a rain percentage but not how much rain, when, or whether it is an official warning. The system shows real forecasts, screens heavy-rain risk
with transparent configurable rules, trains a separate evaluated ML model, and keeps **provider forecast / rule-based screening / ML estimate / official warning** visibly distinct.
See [`docs/project-report.md`](docs/project-report.md) for abstract, objectives, viva questions and demo script.

## Features
Dashboard · 7-day forecast · hourly rainfall · rainfall analysis · risk map · alert centre (official vs system estimate) · history with saved-forecast comparison · model page · admin alert management ·
stale-data and provider-failure handling · rate limiting · OpenAPI docs. *(Screenshots: add your own here after running.)*

## Technology
React 18 + Vite + Tailwind 3 + React Router + Axios + Recharts + Lucide + Leaflet · Java 21, Spring Boot 3.3 (Web, Data JPA, Security, Validation, Actuator), Flyway, MySQL 8 ·
Python (tested 3.12), pandas, NumPy, scikit-learn, joblib, FastAPI, Uvicorn, pytest.

Architecture: [`docs/architecture.md`](docs/architecture.md) · API: [`docs/api-documentation.md`](docs/api-documentation.md) · Data & licences: [`docs/data-sources.md`](docs/data-sources.md) ·
Methodology: [`docs/model-methodology.md`](docs/model-methodology.md) · Database/ERD: [`docs/database-erd.md`](docs/database-erd.md) · Deployment: [`docs/deployment.md`](docs/deployment.md)

## Ubuntu setup (step by step)

Ports: frontend 5173, backend 8080, ML 8000, MySQL 3306 (all configurable).

**1. Install tools** (Java 21, Maven, Node, Python, MySQL)
```bash
bash scripts/setup_ubuntu.sh
java -version && mvn -v && node -v && python3 --version && mysql --version
```
**2. Configure**
```bash
cp .env.example .env
nano .env              # at least set DB_PASSWORD
bash scripts/create_database.sh
```
**3. ML service** – test, then (with internet) download real data and train
```bash
cd ml-service
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
python -m pytest -q                      # expect: 22 passed
cd .. && bash scripts/train_model.sh     # downloads 2010-2024 data for 8 TN cities, trains, writes ml-service/models/
cat ml-service/models/metadata.json      # REAL metrics vs baselines
bash scripts/run_ml.sh                   # starts http://localhost:8000  (docs: /docs)
```
If there are too few heavy-rain days, the classifier is reported as *pending*; the regression model still trains.

**4. Backend** (new terminal; creates tables with Flyway on first start)
```bash
cd backend && mvn test                   # first run downloads dependencies
cd .. && bash scripts/run_backend.sh     # http://localhost:8080  (Swagger: /swagger-ui.html)
```
**5. Frontend** (new terminal)
```bash
bash scripts/run_frontend.sh             # http://localhost:5173
cd frontend && npm test && npm run build # optional checks
```
**6. Verify all three**
```bash
bash scripts/smoke_test.sh
```
**7. Enable the admin page** (optional)
```bash
bash scripts/hash_password.sh 'a-long-password'     # copy the output
nano .env      # ADMIN_PASSWORD_BCRYPT='<paste hash inside single quotes>'  then restart the backend
```
Open http://localhost:5173/admin. Record official warnings **with their source URL**; they are labelled "administrator-entered, not automatically verified".

**Stop / restart:** press `Ctrl+C` in each terminal, run the `run_*.sh` script again. MySQL: `sudo systemctl stop|start mysql`.

## Environment variables
All in [`.env.example`](.env.example): `DB_URL, DB_USERNAME, DB_PASSWORD, SERVER_PORT, CORS_ALLOWED_ORIGINS, ML_SERVICE_URL, ML_SERVICE_API_KEY, ADMIN_USERNAME, ADMIN_PASSWORD_BCRYPT,
FORECAST_CACHE_TTL_MINUTES, SNAPSHOT_RETENTION_DAYS, RATE_LIMIT_PER_MINUTE, RISK_*` (screening thresholds), `ML_CORS_ALLOWED_ORIGINS, ML_MODELS_DIR`, `VITE_API_BASE_URL`.
Never commit `.env`.

## Common errors and fixes
| Symptom | Fix |
|---|---|
| `Port 8080 already in use` | `ss -ltnp \| grep 8080`, stop that process or set `SERVER_PORT=8081` (and `VITE_API_BASE_URL`, `CORS_ALLOWED_ORIGINS`). |
| `Access denied for user 'weather_app'` | `DB_PASSWORD` in `.env` differs from the MySQL user; rerun `scripts/create_database.sh`. Load the env: scripts do it, plain `mvn` does not (`set -a; . ./.env; set +a`). |
| Frontend says "Cannot reach the weather API" | Backend not running, wrong `VITE_API_BASE_URL`, or its origin is missing from `CORS_ALLOWED_ORIGINS`. |
| "The weather provider is not reachable" | No internet / Open-Meteo down or rate-limited. A recent forecast is shown as *stale* if available. |
| About-model page: "Model not available" | Run `scripts/train_model.sh` then restart the ML service. |
| `/predict/heavy-rain` 503 | Classifier not trained (too few heavy-rain days) – see `metadata.json`. |
| Admin sign-in always fails | `ADMIN_PASSWORD_BCRYPT` empty (admin disabled), unquoted `$` in `.env`, or backend not restarted. |
| `mvn` compile error | The backend was not compiled by its author; read the first error, fix, rerun. |
| `npm` warns about `prefix` | Harmless; remove the `prefix` line from `~/.npmrc` if you wish. |

## Testing
```bash
cd ml-service && . .venv/bin/activate && python -m pytest -q
cd frontend && npm test
cd backend && mvn test
bash scripts/smoke_test.sh        # needs all services running
```

## Limitations
Forecasts and screening are estimates and can be wrong; no flood modelling; ML model is 1-day-ahead from past observations on reanalysis data; heavy-rain labels are sparse;
no automatic IMD import; browser-stored saved locations; no notifications or user accounts in this release; backend unverified until you build it (see status table).

## Future enhancements
Station (gauge) data, calibrated probabilistic models, permitted IMD feed, e-mail/push notifications, accounts and server-side saved locations, flood-risk layers with validated terrain/drainage data.

## References
Open-Meteo (https://open-meteo.com/en/docs) · IMD (https://mausam.imd.gov.in/) · scikit-learn · Spring Boot · FastAPI · OpenStreetMap. No other citations are claimed.
