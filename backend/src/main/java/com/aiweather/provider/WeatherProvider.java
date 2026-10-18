package com.aiweather.provider;

import com.aiweather.dto.ApiDtos.*;
import java.time.LocalDate;
import java.util.List;

/** Abstraction over a weather data source so another provider can be added later. */
public interface WeatherProvider {
    String name();

    ForecastResponse fetchForecast(Coordinates c);

    /** Past daily values from an archive/reanalysis source; entries can have null values. */
    List<HistoricalDay> fetchHistory(Coordinates c, LocalDate from, LocalDate to);

    /**
     * The most recent run of COMPLETE, consecutive past days (today excluded), oldest first, at most {@code days}.
     * Days with missing hourly data are never filled in.
     */
    List<DailyObservation> fetchRecentDailyObservations(Coordinates c, int days);

    List<GeoResult> geocode(String query, int maxResults);
}
