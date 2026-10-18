# Final-year project material

## Title
AI Weather & Heavy Rain Prediction System – Intelligent Rainfall Forecasting, Heavy-Rain Risk Assessment and Weather Alert Platform

## Abstract
Heavy rain disrupts travel, agriculture and daily life, yet forecast information is scattered and often hard to interpret: rain *probability* is
confused with rain *amount*, and forecasts are confused with official warnings. This project builds a full-stack web application (React, Spring Boot, MySQL, Python/FastAPI) that
retrieves real forecasts from Open-Meteo for Tamil Nadu locations, presents today, tomorrow and the next seven days with hourly detail, and screens for heavy-rain risk using a transparent, configurable rule set.
A separate machine-learning pipeline, trained and evaluated chronologically on historical weather data against simple baselines, estimates next-day rainfall and heavy-rain occurrence.
Official warnings are handled separately: the system links to the India Meteorological Department and lets administrators record warnings with source links, always labelled as administrator-entered.
The system is a decision-support tool, not a warning service, and it reports its limitations and data sources on every screen.
*(Add your measured model results here after running the pipeline; none are claimed in this document.)*

## Problem statement
Users lack a single, honest view that separates provider forecasts, official warnings and model estimates, shows rainfall amounts and timing, and explains how risk was derived.

## Objectives
1. Show real daily and hourly forecasts for a selected location. 2. Screen heavy-rain risk with documented, configurable thresholds.
3. Train and evaluate ML models on historical data without time leakage. 4. Present official alerts distinctly from system estimates.
5. Show historical rainfall and compare with earlier forecasts. 6. Handle failures and missing data transparently.

## Existing-system limitations
Generic weather apps show a percentage without amounts or reasons; many blend model output with warnings; they rarely explain uncertainty or data sources; they rarely let users see historical forecast-vs-actual.

## Proposed methodology
Provider integration with caching and failure handling → rule-based screening → chronological ML evaluation (baselines, PR-AUC, recall) → separate alert workflow → layered testing.

## Modules
Weather dashboard · 7-day forecast · Rainfall analysis · Risk map · Alert centre · Historical weather · Model information · Admin alert management · REST backend · ML service · Database & migrations.

## Software and hardware requirements
Ubuntu 22.04+, Java 21, Maven 3.8+, Node 18+, Python 3.10+ (tested 3.12), MySQL 8, a modern browser. Hardware: 4 GB RAM minimum (8 GB recommended to run all services), ~2 GB disk, internet access.

## Testing strategy
Unit tests (feature engineering, risk thresholds, provider parsing), API tests (valid/invalid input, missing model, security), UI tests (states, labelling), migration tests on real MySQL, smoke script for the integrated system. Actual results: `testing-report.md`.

## Presentation outline (10–12 slides)
1. Title and problem · 2. Why existing apps confuse users · 3. Objectives · 4. Architecture · 5. Data sources · 6. Dashboard demo · 7. Risk method and thresholds · 8. ML pipeline and leakage prevention · 9. Results (your real numbers, with baselines) · 10. Alerts: official vs estimate · 11. Testing and limitations · 12. Future work.

## Demo script (≈5 min)
1. Open the dashboard, select Chennai, point out the source and "fetched" time. 2. Show the rain ribbon and explain probability vs mm. 3. Open Risk map; explain colour + text.
4. Open Alerts; show the IMD link and the "not automatically verified" label. 5. Admin page: sign in, add a TEST alert, see it appear labelled. 6. History page: archive vs saved forecast.
7. About the model: show real metrics vs baselines, or the honest "not trained" state. 8. Stop the backend to show the honest error + retry.

## Viva questions with simple answers
* **Is this an official warning system?** No. It is decision support; official warnings come from IMD and are only recorded by an administrator with a source link.
* **Rain probability vs rainfall amount?** Probability is the chance of any measurable rain; amount is how much. A high chance can still mean a drizzle.
* **Why not a random train/test split?** Weather is a time series; random splits let the model see the future. We split by date with a purge gap.
* **What is data leakage and how did you avoid it?** Using information unavailable at prediction time. Features use only day D and earlier; preprocessing is fitted on training rows only; a test proves future changes don't alter past features.
* **Why compare with baselines?** To show the model adds value over "tomorrow = today" or the average.
* **Why recall for heavy rain?** A missed heavy-rain day (false negative) costs more than a false alarm.
* **Why is PR-AUC used?** Heavy-rain days are rare; accuracy would look high by predicting "no".
* **Are model scores probabilities?** Not unless calibrated; we show `score_is_calibrated_probability:false`.
* **Where do the thresholds come from?** Configuration; defaults follow commonly cited IMD classes and must be verified; hourly and probability rules are our own.
* **What if the weather API is down?** Retries with limits; a recent cached forecast is shown with a visible "stale" warning; otherwise a clear error. Nothing is invented.
* **Why a separate ML service?** Python's ML libraries; independent deployment; the backend stays Java.
* **Can it predict floods?** No: it needs terrain, drainage and river data.
* **How is the admin API protected?** HTTP Basic over HTTPS, BCrypt hash from the environment, no hash = disabled, rate limiting, https-only source URLs.
* **Limitations?** Reanalysis not gauge data, 1-day horizon, train/serve data difference, backend untested in the generation environment.
* **Future work?** Station data, probabilistic/calibrated models, IMD feed if permitted, e-mail/push notifications, user accounts.

## References (real sources only)
Open-Meteo documentation (https://open-meteo.com/en/docs) · IMD (https://mausam.imd.gov.in/) · scikit-learn (https://scikit-learn.org/) · Spring Boot (https://spring.io/projects/spring-boot) · FastAPI (https://fastapi.tiangolo.com/) · OpenStreetMap (https://www.openstreetmap.org/copyright).
Add academic references you have actually read; none are invented here.
