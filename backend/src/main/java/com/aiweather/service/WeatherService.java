package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.provider.ProviderUnavailableException;
import com.aiweather.provider.WeatherProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final WeatherProvider provider;
    private final ForecastCache cache;
    private final SnapshotService snapshots;
    private final Clock clock;
    private final Duration ttl;
    private final Duration maxStale;

    public WeatherService(WeatherProvider provider, ForecastCache cache, SnapshotService snapshots, Clock clock, AppProperties props) {
        this.provider = provider;
        this.cache = cache;
        this.snapshots = snapshots;
        this.clock = clock;
        this.ttl = Duration.ofMinutes(props.cache().forecastTtlMinutes());
        this.maxStale = Duration.ofHours(props.cache().forecastMaxStaleHours());
    }

    /** Fresh-enough cached forecast, else a provider call. On provider failure a recent cached forecast is returned flagged stale=true. */
    public ForecastResponse forecast(Coordinates c) {
        String key = c.key();
        Instant now = clock.instant();
        Optional<ForecastCache.Entry> cached = cache.get(key);
        if (cached.isPresent() && Duration.between(cached.get().storedAt(), now).compareTo(ttl) < 0) {
            return cached.get().forecast();
        }
        try {
            ForecastResponse fresh = provider.fetchForecast(c);
            cache.put(key, fresh, now);
            try {
                snapshots.save(fresh);
            } catch (RuntimeException e) { // the user still gets the forecast if storage fails
                log.warn("Could not store forecast snapshot: {}", e.toString());
            }
            return fresh;
        } catch (ProviderUnavailableException e) {
            if (cached.isPresent() && Duration.between(cached.get().storedAt(), now).compareTo(maxStale) < 0) {
                log.warn("Serving stale forecast for {} because the provider failed: {}", key, e.getMessage());
                return cached.get().forecast().withStale(true);
            }
            throw e;
        }
    }

    public CurrentResponse current(Coordinates c) {
        ForecastResponse f = forecast(c);
        return new CurrentResponse(f.location(), f.provider(), f.providerUrl(), f.fetchedAt(), f.stale(), f.current());
    }

    public HourlyResponse hourly(Coordinates c) {
        ForecastResponse f = forecast(c);
        return new HourlyResponse(f.location(), f.provider(), f.providerUrl(), f.fetchedAt(), f.stale(), f.hourly());
    }

    public List<GeoResult> search(String q) {
        return provider.geocode(q, 10);
    }
}
