# Data sources and licensing

| Use | Source | Notes |
|---|---|---|
| Forecasts (current, hourly, 7-day) | Open-Meteo Forecast API – https://open-meteo.com/en/docs | Free API is for non-commercial use; attribution required. Check https://open-meteo.com/en/terms before any commercial/production use. Request limits apply (the app caches and handles HTTP 429). |
| Geocoding | Open-Meteo Geocoding API – https://open-meteo.com/en/docs/geocoding-api | Used for city/district search. |
| Historical rainfall (History page, ML training) | Open-Meteo Historical Weather API – https://open-meteo.com/en/docs/historical-weather-api | **Reanalysis** (gridded model + assimilated observations), not rain-gauge data. Recent days can be missing or revised. Data licence: CC BY 4.0 (verify on the site). |
| Official warnings | India Meteorological Department – https://mausam.imd.gov.in/ | **No machine-readable IMD feed was verified**, so nothing is imported automatically. The UI links to the official site; an administrator may record a warning with its source URL. Verify the link and terms of use yourself. |
| Map tiles | OpenStreetMap – https://www.openstreetmap.org/copyright | Attribution shown on the map. The public tile server has a usage policy; use a tile provider for heavy traffic. |

## ML training data provenance
`python -m training.fetch_data` writes `ml-service/data/processed/daily.csv` and `provenance.json` containing the source URL,
collection timestamp (UTC), requested period, timezone, variable units, locations, row count and SHA-256 of the CSV.
Units: temperature °C, humidity %, surface pressure hPa, wind km/h, precipitation mm.
Daily values are local-calendar-day aggregates (sum of precipitation; mean of temperature/humidity/pressure; max of wind); days with fewer than 24 hourly values become missing.
**The data has not been downloaded in the environment where this repository was generated (no access to Open-Meteo), so no data file or trained model ships with the repo.**

## Known data limitations
* Reanalysis grid cells smooth extremes: very heavy point rainfall is under-represented, so heavy-rain events are rare in the training labels.
* Serving uses the forecast API's recent hourly values, whereas training uses the archive (reanalysis). They are related but not identical products (a train/serve distribution difference).
* City coordinates in the quick-pick list are approximate city centres.
