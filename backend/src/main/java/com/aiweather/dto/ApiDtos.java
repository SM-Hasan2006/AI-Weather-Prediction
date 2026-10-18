package com.aiweather.dto;

import com.aiweather.config.AppProperties;
import com.aiweather.entity.AlertOrigin;
import com.aiweather.entity.AlertSeverity;
import com.aiweather.entity.RiskCategory;
import com.aiweather.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Request/response shapes of the public REST API (the contract the React frontend consumes). */
public final class ApiDtos {
    private ApiDtos() {}

    /** Validated coordinates rounded to 2 decimals (~1 km) for caching and storage. */
    public record Coordinates(double latitude, double longitude) {
        public static Coordinates of(double lat, double lon) {
            if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "latitude must be a number between -90 and 90.");
            }
            if (!Double.isFinite(lon) || lon < -180 || lon > 180) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "longitude must be a number between -180 and 180.");
            }
            return new Coordinates(Math.round(lat * 100.0) / 100.0, Math.round(lon * 100.0) / 100.0);
        }

        public String key() {
            return String.format(java.util.Locale.ROOT, "%.2f,%.2f", latitude, longitude);
        }
    }

    public record ErrorResponse(Instant timestamp, int status, String code, String message, List<FieldIssue> details) {}

    public record FieldIssue(String field, String message) {}

    // ---- locations
    public record GeoResult(String name, String admin1, String admin2, String countryCode,
                            double latitude, double longitude, String timezone) {}

    // ---- weather
    public record LocationInfo(double latitude, double longitude, String timezone) {}

    public record CurrentWeather(String time, Double temperatureC, Integer relativeHumidityPct, Double precipitationMm,
                                 Double windSpeedKmh, Integer weatherCode) {}

    public record DailyForecast(LocalDate date, Integer weatherCode, Double temperatureMaxC, Double temperatureMinC,
                                Double precipitationSumMm, Integer precipitationProbabilityMaxPct, Double windSpeedMaxKmh) {}

    public record HourlyForecast(String time, Double temperatureC, Double precipitationMm,
                                 Integer precipitationProbabilityPct, Double windSpeedKmh, Integer weatherCode) {}

    public record ForecastResponse(LocationInfo location, String provider, String providerUrl, Instant fetchedAt,
                                   LocalDate forecastStart, LocalDate forecastEnd, boolean stale,
                                   CurrentWeather current, List<DailyForecast> daily, List<HourlyForecast> hourly) {
        public ForecastResponse withStale(boolean value) {
            return new ForecastResponse(location, provider, providerUrl, fetchedAt, forecastStart, forecastEnd,
                    value, current, daily, hourly);
        }
    }

    public record CurrentResponse(LocationInfo location, String provider, String providerUrl, Instant fetchedAt,
                                  boolean stale, CurrentWeather current) {}

    public record HourlyResponse(LocationInfo location, String provider, String providerUrl, Instant fetchedAt,
                                 boolean stale, List<HourlyForecast> hourly) {}

    // ---- history
    public record HistoricalDay(LocalDate date, Double precipitationSumMm) {}

    public record HistoryDay(LocalDate date, Double precipitationSumMm, Double storedForecastPrecipitationMm) {}

    public record HistoryResponse(LocationInfo location, String provider, String providerUrl, String dataDescription,
                                  Instant fetchedAt, LocalDate from, LocalDate to, List<HistoryDay> daily) {}

    // ---- ML input (snake_case = the Python service's schema)
    public record DailyObservation(
            String date,
            @JsonProperty("precipitation_sum_mm") double precipitationSumMm,
            @JsonProperty("temperature_mean_c") double temperatureMeanC,
            @JsonProperty("humidity_mean_pct") double humidityMeanPct,
            @JsonProperty("pressure_mean_hpa") double pressureMeanHpa,
            @JsonProperty("wind_max_kmh") double windMaxKmh) {}

    // ---- risk
    public record DayRisk(LocalDate date, RiskCategory category, Double precipitationSumMm, Integer probabilityPct) {}

    public record Thresholds(double moderateDailyMm, double highDailyMm, double veryHighDailyMm, double highHourlyMm,
                             int minProbabilityPct, String note) {
        public static Thresholds from(AppProperties.Risk r) {
            return new Thresholds(r.moderateDailyMm(), r.highDailyMm(), r.veryHighDailyMm(), r.highHourlyMm(),
                    r.minProbabilityPct(),
                    "Screening thresholds on forecast rainfall, not official warnings. Daily classes follow commonly cited IMD rainfall categories; "
                            + "hourly intensity and the rain-chance gate are application-defined.");
        }
    }

    public record RiskResponse(RiskCategory category, String method, String methodVersion, String source,
                               Instant assessedAt, Instant forecastFetchedAt, boolean stale,
                               LocalDate windowStart, LocalDate windowEnd, Double maxDailyPrecipMm,
                               List<String> reasons, List<DayRisk> perDay, Thresholds thresholds,
                               boolean floodRiskAssessed, String disclaimer) {}

    // ---- alerts
    public record AlertDto(Long id, AlertOrigin origin, String title, String message, AlertSeverity severity,
                           String sourceName, String sourceUrl, Instant issuedAt, Instant validFrom, Instant validTo,
                           String affectedAreas, Double centerLatitude, Double centerLongitude, Double radiusKm,
                           @JsonProperty("isTest") boolean isTest) {}

    public record AlertsResponse(List<AlertDto> official, List<AlertDto> systemGenerated, String systemGeneratedNote,
                                 List<AppProperties.Link> officialLinks, String disclaimer) {}

    public record AlertRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 4000) String message,
            @NotNull AlertSeverity severity,
            @NotBlank @Size(max = 120) String sourceName,
            @NotBlank @Size(max = 500) @Pattern(regexp = "^https://\\S+$", message = "must be an https:// URL") String sourceUrl,
            @NotNull Instant issuedAt,
            @NotNull Instant validFrom,
            @NotNull Instant validTo,
            @NotBlank @Size(max = 500) String affectedAreas,
            Double centerLatitude,
            Double centerLongitude,
            Double radiusKm,
            @JsonProperty("isTest") boolean isTest) {}

    // ---- ML
    public record MlStatusResponse(boolean available, String reason, JsonNode model) {}

    public record MlPredictionResponse(JsonNode rainfall, JsonNode heavyRain, List<String> messages) {}

    // ---- health
    public record HealthResponse(String status, String database, String mlService, Instant time) {}
}
