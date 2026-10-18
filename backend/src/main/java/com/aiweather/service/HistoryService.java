package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.entity.ForecastDaily;
import com.aiweather.entity.LocationEntity;
import com.aiweather.provider.WeatherProvider;
import com.aiweather.repository.ForecastDailyRepository;
import com.aiweather.repository.LocationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HistoryService {
    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);
    public static final int MAX_DAYS = 92;

    private final WeatherProvider provider;
    private final LocationRepository locations;
    private final ForecastDailyRepository dailyRepo;
    private final Clock clock;
    private final AppProperties props;

    public HistoryService(WeatherProvider provider, LocationRepository locations, ForecastDailyRepository dailyRepo,
                          Clock clock, AppProperties props) {
        this.provider = provider;
        this.locations = locations;
        this.dailyRepo = dailyRepo;
        this.clock = clock;
        this.props = props;
    }

    public HistoryResponse history(Coordinates c, int days) {
        if (days < 1 || days > MAX_DAYS) {
            throw new com.aiweather.exception.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "invalid_input",
                    "days must be between 1 and " + MAX_DAYS + ".");
        }
        LocalDate to = LocalDate.now(clock).minusDays(1);
        LocalDate from = to.minusDays(days - 1L);
        List<HistoricalDay> archive = provider.fetchHistory(c, from, to);
        Map<LocalDate, Double> stored = storedForecasts(c, from, to);
        List<HistoryDay> out = new ArrayList<>();
        for (HistoricalDay d : archive) {
            out.add(new HistoryDay(d.date(), d.precipitationSumMm(), stored.get(d.date())));
        }
        return new HistoryResponse(new LocationInfo(c.latitude(), c.longitude(), "auto"), "Open-Meteo Historical Weather API",
                props.providers().openMeteo().publicUrl(),
                "Archive values are a gridded reanalysis (model-assimilated estimate), not rain-gauge measurements, and can differ from what fell at one spot.",
                clock.instant(), from, to, out);
    }

    /** For each date: the rainfall forecast saved by this system BEFORE that date began (latest such snapshot). */
    private Map<LocalDate, Double> storedForecasts(Coordinates c, LocalDate from, LocalDate to) {
        Map<LocalDate, Double> result = new HashMap<>();
        try {
            Optional<LocationEntity> loc = locations.findByLatitudeAndLongitude(SnapshotService.coord(c.latitude()), SnapshotService.coord(c.longitude()));
            if (loc.isEmpty()) return result;
            Map<LocalDate, java.time.Instant> newest = new HashMap<>();
            for (ForecastDaily d : dailyRepo.findForLocationBetween(loc.get().getId(), from, to)) {
                if (d.getPrecipitationSumMm() == null) continue;
                LocalDate fetchedDay = d.getSnapshot().getFetchedAt().atZone(ZoneId.of(d.getSnapshot().getTimezone())).toLocalDate();
                if (!fetchedDay.isBefore(d.getForecastDate())) continue; // made on/after the day: not a real forecast
                java.time.Instant prev = newest.get(d.getForecastDate());
                if (prev == null || d.getSnapshot().getFetchedAt().isAfter(prev)) {
                    newest.put(d.getForecastDate(), d.getSnapshot().getFetchedAt());
                    result.put(d.getForecastDate(), d.getPrecipitationSumMm().doubleValue());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Could not read stored forecasts: {}", e.toString());
        }
        return result;
    }
}
