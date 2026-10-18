# Testing report

Generated in a sandbox with Ubuntu 24.04, Java 21, Node 22, Python 3.12, MySQL 8.0.46. The sandbox **could reach npm and PyPI but not
Open-Meteo, IMD or Maven Central.** That determines what could and could not be verified. Only real outcomes are listed.

## Verified by execution

| Area | Command | Result |
|---|---|---|
| ML service (pipeline + API) | `cd ml-service && . .venv/bin/activate && python -m pytest -q` | **22 passed** |
| Frontend (components, utilities, pages) | `cd frontend && npm test` | **29 passed** (5 test files) |
| Frontend production build | `cd frontend && npm run build` | succeeds (one chunk-size warning, 838 kB) |
| MySQL migration | `mysql … < V1__core_schema.sql` on MySQL 8.0.46 | all 5 tables created; CHECK (lat range, alert period, alert area), UNIQUE (coordinates) violations rejected as designed; valid inserts accepted; cascade delete removed child rows |
| ML service smoke test | `uvicorn app.main:app` then `curl` | `/health` 200 (`model_available:false`), `/model/info` `available:false` + reason, invalid body 422, valid body without model **503 `model_unavailable`** |
| Honest failure of data scripts | `python -m training.fetch_data` (no network) / `python -m training.train --data /nonexistent.csv` | download aborts with an error and writes no file; training exits 1 with instructions |
| Shell scripts | `bash -n scripts/*.sh` | no syntax errors; `hash_password.sh` produces a BCrypt hash |

What the ML tests cover: feature lags and rolling windows, **no future leakage into features**, missing calendar days, next-day target
alignment, incomplete hourly days, chronological split with purge gap, artefacts + metadata, classifier marked *pending* when heavy-rain examples are too few,
data-quality checks, API valid/invalid inputs, impossible values, date gaps, missing model files (503), API key, CORS allow-list.
They use **synthetic data only to exercise mechanics**; no accuracy is claimed from them.

What the frontend tests cover: formatting and missing values (shown as "—", not 0), risk labels (text + colour, unknown ≠ low), API error mapping,
official vs system alert labelling, TEST label, stale-data notice, insufficient-data state, error + retry state, location search/selection, wrong admin login, "system estimates unavailable" note.
API calls are mocked **in tests only**.

## NOT verified (be explicit when presenting)

| Item | Why | What exists |
|---|---|---|
| **Spring Boot backend compile and tests** | Maven Central unreachable, so dependencies cannot be downloaded | 44 Java files; `javac` parses all of them (the only errors are missing-library errors, no syntax errors). Type-checking, Spring wiring and the JUnit tests (RiskEngine thresholds, provider parsing/retry/429, cache + stale fallback, security/validation via MockMvc) have **not been run**. Expect to fix small compile errors on first `mvn test`. |
| Flyway running inside Spring | same | SQL itself verified manually (above) |
| Real Open-Meteo calls (forecast, geocoding, archive) | no network access to the provider | code and response parsing written against the documented format and tested only with mocked responses |
| ML training on real data and any accuracy figure | no data could be downloaded | pipeline complete; **model training is pending** |
| Backend ↔ ML ↔ frontend end-to-end, browser rendering/screenshots | backend not runnable | `scripts/smoke_test.sh` is provided for this |
| Repository/database integration tests from Java | no Java build | schema tested directly in MySQL |
| Deployment | no hosting credentials | guide in `deployment.md`, nothing deployed |
| Accessibility audit with real tools | not run | semantic HTML, labels, focus rings, skip link, reduced-motion CSS implemented |

## Next steps to complete verification (on your Ubuntu machine)
1. `cd backend && mvn test` → fix any compile error, then `mvn spring-boot:run`.
2. `bash scripts/train_model.sh` → read real metrics in `ml-service/models/metadata.json`.
3. Start all three services and run `bash scripts/smoke_test.sh`.
4. Update this file with your actual results.
