package com.aiweather.provider;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/** Open-Meteo (https://open-meteo.com/) forecast, archive and geocoding APIs. */
@Component
public class OpenMeteoProvider implements WeatherProvider {
    private static final Logger log = LoggerFactory.getLogger(OpenMeteoProvider.class);

    private final RestClient http;
    private final AppProperties.OpenMeteo cfg;
    private final Clock clock;

    public OpenMeteoProvider(@Qualifier("weatherRestClient") RestClient http, AppProperties props, Clock clock) {
        this.http = http;
        this.cfg = props.providers().openMeteo();
        this.clock = clock;
    }

    @Override
    public String name() {
        return "Open-Meteo";
    }

    // ------------------------------------------------------------------ forecast
    @Override
    public ForecastResponse fetchForecast(Coordinates c) {
        URI uri = UriComponentsBuilder.fromUriString(cfg.forecastUrl())
                .queryParam("latitude", c.latitude())
                .queryParam("longitude", c.longitude())
                .queryParam("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m")
                .queryParam("hourly", "temperature_2m,precipitation_probability,precipitation,wind_speed_10m,weather_code")
                .queryParam("daily", "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max,wind_speed_10m_max")
                .queryParam("forecast_days", 7)
                .queryParam("timezone", "auto")
                .queryParam("wind_speed_unit", "kmh")
                .build().toUri();
        return parseForecast(getJson(uri), c);
    }

    ForecastResponse parseForecast(JsonNode root, Coordinates c) {
        JsonNode daily = root.path("daily");
        JsonNode times = daily.path("time");
        if (!times.isArray() || times.isEmpty()) {
            throw new ProviderUnavailableException("The weather provider returned no daily forecast for this location.");
        }
        List<DailyForecast> days = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            days.add(new DailyForecast(LocalDate.parse(times.get(i).asText()),
                    integer(daily, "weather_code", i), dbl(daily, "temperature_2m_max", i), dbl(daily, "temperature_2m_min", i),
                    dbl(daily, "precipitation_sum", i), integer(daily, "precipitation_probability_max", i),
                    dbl(daily, "wind_speed_10m_max", i)));
        }
        List<HourlyForecast> hours = new ArrayList<>();
        JsonNode hourly = root.path("hourly");
        JsonNode ht = hourly.path("time");
        if (ht.isArray()) {
            for (int i = 0; i < ht.size(); i++) {
                hours.add(new HourlyForecast(ht.get(i).asText(), dbl(hourly, "temperature_2m", i), dbl(hourly, "precipitation", i),
                        integer(hourly, "precipitation_probability", i), dbl(hourly, "wind_speed_10m", i), integer(hourly, "weather_code", i)));
            }
        }
        JsonNode cur = root.path("current");
        CurrentWeather current = cur.isObject()
                ? new CurrentWeather(cur.path("time").asText(null), scalarD(cur, "temperature_2m"), scalarI(cur, "relative_humidity_2m"),
                        scalarD(cur, "precipitation"), scalarD(cur, "wind_speed_10m"), scalarI(cur, "weather_code"))
                : null;
        String tz = root.path("timezone").asText("UTC");
        return new ForecastResponse(new LocationInfo(c.latitude(), c.longitude(), tz), name(), cfg.publicUrl(), clock.instant(),
                days.get(0).date(), days.get(days.size() - 1).date(), false, current, days, hours);
    }

    // ------------------------------------------------------------------ history (archive / reanalysis)
    @Override
    public List<HistoricalDay> fetchHistory(Coordinates c, LocalDate from, LocalDate to) {
        URI uri = UriComponentsBuilder.fromUriString(cfg.archiveUrl())
                .queryParam("latitude", c.latitude()).queryParam("longitude", c.longitude())
                .queryParam("start_date", from).queryParam("end_date", to)
                .queryParam("daily", "precipitation_sum").queryParam("timezone", "auto")
                .build().toUri();
        JsonNode daily = getJson(uri).path("daily");
        JsonNode times = daily.path("time");
        if (!times.isArray()) {
            throw new ProviderUnavailableException("The weather provider returned no historical data for this location.");
        }
        List<HistoricalDay> out = new ArrayList<>();
        for (int i = 0; i < times.size(); i++) {
            out.add(new HistoricalDay(LocalDate.parse(times.get(i).asText()), dbl(daily, "precipitation_sum", i)));
        }
        return out;
    }

    // ------------------------------------------------------------------ recent observations for the ML model
    @Override
    public List<DailyObservation> fetchRecentDailyObservations(Coordinates c, int days) {
        URI uri = UriComponentsBuilder.fromUriString(cfg.forecastUrl())
                .queryParam("latitude", c.latitude()).queryParam("longitude", c.longitude())
                .queryParam("hourly", "temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,precipitation")
                .queryParam("past_days", days + 2).queryParam("forecast_days", 1)
                .queryParam("timezone", "auto").queryParam("wind_speed_unit", "kmh")
                .build().toUri();
        JsonNode root = getJson(uri);
        JsonNode h = root.path("hourly");
        JsonNode times = h.path("time");
        if (!times.isArray()) {
            throw new ProviderUnavailableException("The weather provider returned no recent observations.");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(root.path("timezone").asText("UTC"))));
        Map<LocalDate, List<Integer>> byDay = new LinkedHashMap<>();
        for (int i = 0; i < times.size(); i++) {
            byDay.computeIfAbsent(LocalDate.parse(times.get(i).asText().substring(0, 10)), k -> new ArrayList<>()).add(i);
        }
        List<DailyObservation> complete = new ArrayList<>();
        for (Map.Entry<LocalDate, List<Integer>> e : byDay.entrySet()) {
            if (!e.getKey().isBefore(today)) continue; // today is incomplete / partly forecast
            DailyObservation obs = aggregateDay(e.getKey(), e.getValue(), h);
            if (obs != null) complete.add(obs);
        }
        // keep the latest run of consecutive days, newest last
        List<DailyObservation> run = new ArrayList<>();
        for (int i = complete.size() - 1; i >= 0 && run.size() < days; i--) {
            if (!run.isEmpty() && !LocalDate.parse(complete.get(i).date()).plusDays(1).equals(LocalDate.parse(run.get(0).date()))) break;
            run.add(0, complete.get(i));
        }
        return run;
    }

    /** Same aggregation as training: precip = sum (24 values required), temp/humidity/pressure = mean, wind = max. */
    static DailyObservation aggregateDay(LocalDate day, List<Integer> idx, JsonNode h) {
        double precip = 0, temp = 0, hum = 0, pres = 0, windMax = Double.NEGATIVE_INFINITY;
        int nP = 0, nT = 0, nH = 0, nPr = 0, nW = 0;
        for (int i : idx) {
            Double p = dbl(h, "precipitation", i), t = dbl(h, "temperature_2m", i), r = dbl(h, "relative_humidity_2m", i),
                    s = dbl(h, "surface_pressure", i), w = dbl(h, "wind_speed_10m", i);
            if (p != null) { precip += p; nP++; }
            if (t != null) { temp += t; nT++; }
            if (r != null) { hum += r; nH++; }
            if (s != null) { pres += s; nPr++; }
            if (w != null) { windMax = Math.max(windMax, w); nW++; }
        }
        if (nP < 24 || nT == 0 || nH == 0 || nPr == 0 || nW == 0) return null;
        return new DailyObservation(day.toString(), Math.round(precip * 10.0) / 10.0, temp / nT, hum / nH, pres / nPr, windMax);
    }

    // ------------------------------------------------------------------ geocoding
    @Override
    public List<GeoResult> geocode(String query, int maxResults) {
        URI uri = UriComponentsBuilder.fromUriString(cfg.geocodingUrl())
                .queryParam("name", query).queryParam("count", maxResults)
                .queryParam("language", "en").queryParam("format", "json")
                .encode().build().toUri();
        JsonNode results = getJson(uri).path("results"); // key is absent when nothing matches
        List<GeoResult> out = new ArrayList<>();
        if (results.isArray()) {
            for (JsonNode r : results) {
                if (!r.path("latitude").isNumber() || !r.path("longitude").isNumber()) continue;
                out.add(new GeoResult(r.path("name").asText(""), r.path("admin1").asText(null), r.path("admin2").asText(null),
                        r.path("country_code").asText(null), r.path("latitude").asDouble(), r.path("longitude").asDouble(),
                        r.path("timezone").asText(null)));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ plumbing
    private JsonNode getJson(URI uri) {
        int attempts = Math.max(1, cfg.maxAttempts());
        RuntimeException last = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                JsonNode body = http.get().uri(uri).accept(MediaType.APPLICATION_JSON).retrieve().body(JsonNode.class);
                if (body == null || body.isNull()) {
                    throw new ProviderUnavailableException("The weather provider returned an empty response.");
                }
                return body;
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode().value() == 429) {
                    throw new ProviderUnavailableException("The weather provider's request limit was reached. Please try again in a few minutes.", e);
                }
                throw new ProviderUnavailableException("The weather provider rejected the request (HTTP " + e.getStatusCode().value() + ").", e);
            } catch (HttpServerErrorException | ResourceAccessException e) {
                last = e;
                log.warn("Weather provider attempt {}/{} failed: {}", attempt, attempts, e.getClass().getSimpleName());
                if (attempt < attempts) sleep((long) cfg.retryBackoffMs() * attempt);
            } catch (RestClientException e) {
                throw new ProviderUnavailableException("The weather provider returned data in an unexpected format.", e);
            }
        }
        throw new ProviderUnavailableException("The weather provider is not reachable right now.", last);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // null-safe accessors: a missing or null value stays null (never silently becomes 0)
    static Double dbl(JsonNode obj, String field, int i) {
        JsonNode a = obj.path(field);
        return a.isArray() && i < a.size() && a.get(i).isNumber() ? a.get(i).asDouble() : null;
    }

    static Integer integer(JsonNode obj, String field, int i) {
        Double d = dbl(obj, field, i);
        return d == null ? null : (int) Math.round(d);
    }

    static Double scalarD(JsonNode obj, String field) {
        return obj.path(field).isNumber() ? obj.path(field).asDouble() : null;
    }

    static Integer scalarI(JsonNode obj, String field) {
        return obj.path(field).isNumber() ? obj.path(field).asInt() : null;
    }
}
