package com.aiweather.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.provider.ProviderUnavailableException;
import com.aiweather.provider.WeatherProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class WeatherServiceTest {
    private final Coordinates c = Coordinates.of(13.08, 80.27);
    private final AppProperties props = new AppProperties(null, null, new AppProperties.Cache(30, 12), null, null, null, null, null, null);

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-18T05:00:00Z");
        public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(java.time.ZoneId z) { return this; }
        public Instant instant() { return now; }
    }

    private ForecastResponse sample() {
        return new ForecastResponse(new LocationInfo(13.08, 80.27, "Asia/Kolkata"), "Open-Meteo", "https://open-meteo.com/", Instant.parse("2026-10-18T05:00:00Z"),
                LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 18), false, null, List.of(), List.of());
    }

    @Test
    void secondCallWithinTtlUsesTheCache() {
        WeatherProvider p = mock(WeatherProvider.class);
        when(p.fetchForecast(any())).thenReturn(sample());
        WeatherService s = new WeatherService(p, new ForecastCache(), mock(SnapshotService.class), new MutableClock(), props);
        s.forecast(c);
        s.forecast(c);
        verify(p, times(1)).fetchForecast(any());
    }

    @Test
    void providerFailureServesRecentCacheMarkedStale() {
        MutableClock clock = new MutableClock();
        WeatherProvider p = mock(WeatherProvider.class);
        when(p.fetchForecast(any())).thenReturn(sample()).thenThrow(new ProviderUnavailableException("down"));
        WeatherService s = new WeatherService(p, new ForecastCache(), mock(SnapshotService.class), clock, props);
        assertFalse(s.forecast(c).stale());
        clock.now = clock.now.plusSeconds(3600); // cache expired, provider now down
        assertTrue(s.forecast(c).stale());
    }

    @Test
    void providerFailureWithNoCacheIsAnError() {
        WeatherProvider p = mock(WeatherProvider.class);
        when(p.fetchForecast(any())).thenThrow(new ProviderUnavailableException("down"));
        WeatherService s = new WeatherService(p, new ForecastCache(), mock(SnapshotService.class), new MutableClock(), props);
        assertThrows(ProviderUnavailableException.class, () -> s.forecast(c));
    }

    @Test
    void tooOldCacheIsNotServed() {
        MutableClock clock = new MutableClock();
        WeatherProvider p = mock(WeatherProvider.class);
        when(p.fetchForecast(any())).thenReturn(sample()).thenThrow(new ProviderUnavailableException("down"));
        WeatherService s = new WeatherService(p, new ForecastCache(), mock(SnapshotService.class), clock, props);
        s.forecast(c);
        clock.now = clock.now.plusSeconds(13 * 3600);
        assertThrows(ProviderUnavailableException.class, () -> s.forecast(c));
    }

    @Test
    void snapshotStorageFailureDoesNotBreakTheResponse() {
        WeatherProvider p = mock(WeatherProvider.class);
        when(p.fetchForecast(any())).thenReturn(sample());
        SnapshotService snaps = mock(SnapshotService.class);
        when(snaps.save(any())).thenThrow(new RuntimeException("db down"));
        WeatherService s = new WeatherService(p, new ForecastCache(), snaps, new MutableClock(), props);
        assertNotNull(s.forecast(c));
    }

    @Test
    void invalidCoordinatesAreRejected() {
        assertThrows(com.aiweather.exception.ApiException.class, () -> Coordinates.of(95, 10));
        assertThrows(com.aiweather.exception.ApiException.class, () -> Coordinates.of(10, 181));
        assertThrows(com.aiweather.exception.ApiException.class, () -> Coordinates.of(Double.NaN, 10));
    }
}
