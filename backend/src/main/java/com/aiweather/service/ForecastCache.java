package com.aiweather.service;

import com.aiweather.dto.ApiDtos.ForecastResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory cache of the last forecast per ~1 km cell. Old entries are kept so they can be served as "stale" on provider failure. */
@Component
public class ForecastCache {
    public record Entry(ForecastResponse forecast, Instant storedAt) {}

    private static final int MAX_ENTRIES = 500;
    private final ConcurrentHashMap<String, Entry> map = new ConcurrentHashMap<>();

    public Optional<Entry> get(String key) {
        return Optional.ofNullable(map.get(key));
    }

    public void put(String key, ForecastResponse forecast, Instant now) {
        if (map.size() >= MAX_ENTRIES) {
            map.entrySet().stream().min(java.util.Map.Entry.comparingByValue(java.util.Comparator.comparing(Entry::storedAt)))
                    .ifPresent(e -> map.remove(e.getKey()));
        }
        map.put(key, new Entry(forecast, now));
    }
}
