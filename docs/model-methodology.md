# Model methodology

## 1. Rule-based risk screening (`RiskEngine`) – not machine learning
Per forecast day, from the provider's 24-hour rainfall total:

| Category | Daily rainfall (default) |
|---|---|
| Low | below 15.6 mm |
| Moderate | ≥ 15.6 mm |
| High | ≥ 64.5 mm |
| Very high | ≥ 115.6 mm |

The daily values match the rainfall classes commonly attributed to IMD (moderate / heavy / very heavy). **They were written from general
knowledge, without access to the IMD documents, so verify them against the current IMD source and change them in `application.yml` /
environment variables if needed.** They are screening thresholds, not official warnings.

Two extra rules are **application-defined, not IMD classes**:
* an hourly amount ≥ `high-hourly-mm` (default 30 mm) lifts a day to at least High;
* if the day's rain chance is below `min-probability-pct` (default 40 %), a High/Very-high amount is capped at Moderate (and the reason says so).

Overall category = the most serious day. A day with no rainfall value is *insufficient data*, never *low*. Every response lists the reasons,
the thresholds used, the method name and version, and the forecast fetch time. Strong wind is mentioned in the reasons but does not change the category.
Probability of precipitation is **not** probability of flooding; flood risk is not assessed (`floodRiskAssessed:false`).

## 2. Machine-learning models (`ml-service/training`)
**Targets** (horizon 1 day): (A) rainfall in mm on day D+1; (B) rainfall on day D+1 ≥ heavy threshold (default 64.5 mm, configurable).

**Features** (day D and earlier only): rainfall lags 0–6 days, rolling 3/7/14-day rainfall sums, rainy days in last 7, mean temperature,
humidity, pressure, 1-day pressure change, max wind, day-of-year sine/cosine, latitude, longitude. The same function (`app/features.py`) builds
features for training and serving. A unit test proves that altering future rows does not change past features.

**Evaluation protocol**
* Chronological split by calendar date (70 % / 15 % / 15 %, same cut dates for all locations) with a 1-day purge gap; no random splits.
* Imputer/scaler live inside the scikit-learn `Pipeline` and are fitted on training rows only.
* Models are chosen on the validation period; the test period is used once for reporting.
* Data checks: duplicates, missing values, negative precipitation, humidity range, missing calendar days.

**Models and baselines**
| Task | Candidates | Baselines |
|---|---|---|
| Regression | RandomForestRegressor | tomorrow = today; training mean |
| Heavy-rain classification | LogisticRegression, RandomForestClassifier (class-weighted) | "never heavy"; heavy today ⇒ heavy tomorrow |

**Metrics**: regression MAE and RMSE; classification precision, recall, F1, confusion matrix, PR-AUC, Brier score and calibration bins.
The decision threshold is chosen on validation (max F1) and then frozen. A false negative (missed heavy-rain day) is the costly error for
warnings, so recall and the false-negative count are reported next to precision.
**Calibration**: scores are *not* calibrated probabilities; the API says `score_is_calibrated_probability:false`.
**Too few positives**: if the training, validation or test period has too few heavy-rain days, the classifier is **not trained** and
`metadata.json` records `pending_insufficient_positive_examples` – nothing is silently substituted.

## 3. Results
**No real evaluation has been run yet** (the generating environment could not reach Open-Meteo). Run `bash scripts/train_model.sh`, then
read `ml-service/models/metadata.json` (or the "About the model" page). Do not quote accuracy before doing so, and compare against the baselines.
The 22 automated ML tests use synthetic data only to test the pipeline mechanics; those numbers are never reported as accuracy.

## 4. Limitations
* One-day-ahead estimate from past observations; it does not see numerical forecasts, so the provider forecast is usually better for tomorrow.
* Predicting rainfall weeks ahead is much less certain than near-term forecasting; the app does not attempt it.
* Reanalysis grid data, not gauge data; heavy-rain labels are sparse.
* A rainfall estimate is not a flood prediction.
