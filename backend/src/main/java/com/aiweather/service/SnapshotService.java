package com.aiweather.service;

import com.aiweather.dto.ApiDtos.DailyForecast;
import com.aiweather.dto.ApiDtos.ForecastResponse;
import com.aiweather.entity.ForecastDaily;
import com.aiweather.entity.ForecastSnapshot;
import com.aiweather.entity.LocationEntity;
import com.aiweather.repository.ForecastSnapshotRepository;
import com.aiweather.repository.LocationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores each freshly fetched forecast so earlier forecasts can later be compared with what happened. */
@Service
public class SnapshotService {
    private final LocationRepository locations;
    private final ForecastSnapshotRepository snapshots;

    public SnapshotService(LocationRepository locations, ForecastSnapshotRepository snapshots) {
        this.locations = locations;
        this.snapshots = snapshots;
    }

    static BigDecimal coord(double v) {
        return BigDecimal.valueOf(v).setScale(5, RoundingMode.HALF_UP);
    }

    static BigDecimal dec(Double v) {
        return v == null ? null : BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public ForecastSnapshot save(ForecastResponse f) {
        BigDecimal lat = coord(f.location().latitude()), lon = coord(f.location().longitude());
        LocationEntity loc = locations.findByLatitudeAndLongitude(lat, lon).orElseGet(() -> {
            LocationEntity n = new LocationEntity();
            n.setName(String.format(java.util.Locale.ROOT, "%.2f,%.2f", f.location().latitude(), f.location().longitude()));
            n.setLatitude(lat);
            n.setLongitude(lon);
            n.setTimezone(f.location().timezone());
            return locations.save(n);
        });
        ForecastSnapshot s = new ForecastSnapshot();
        s.setLocation(loc);
        s.setProvider(f.provider());
        s.setFetchedAt(f.fetchedAt());
        s.setForecastStartDate(f.forecastStart());
        s.setForecastEndDate(f.forecastEnd());
        s.setTimezone(f.location().timezone());
        for (DailyForecast d : f.daily()) {
            ForecastDaily row = new ForecastDaily();
            row.setSnapshot(s);
            row.setForecastDate(d.date());
            row.setPrecipitationSumMm(dec(d.precipitationSumMm()));
            row.setPrecipitationProbabilityMax(d.precipitationProbabilityMaxPct());
            row.setTemperatureMaxC(dec(d.temperatureMaxC()));
            row.setTemperatureMinC(dec(d.temperatureMinC()));
            row.setWindSpeedMaxKmh(dec(d.windSpeedMaxKmh()));
            s.getDays().add(row);
        }
        return snapshots.save(s);
    }
}
