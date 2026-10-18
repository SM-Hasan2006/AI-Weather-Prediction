package com.aiweather.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Uses mocked provider responses (tests only; production always calls the real API). */
class OpenMeteoProviderTest {
    private MockRestServiceServer server;
    private OpenMeteoProvider provider;
    private final Coordinates chennai = Coordinates.of(13.0827, 80.2707);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AppProperties props = new AppProperties(null,
                new AppProperties.Providers(new AppProperties.OpenMeteo("http://om/forecast", "http://om/archive", "http://om/geo",
                        "https://open-meteo.com/", 1000, 1000, 3, 1)),
                null, null, null, null, null, null, null);
        provider = new OpenMeteoProvider(builder.build(), props, Clock.fixed(Instant.parse("2026-10-18T05:00:00Z"), ZoneOffset.UTC));
    }

    private static final String FORECAST_JSON = """
            {"timezone":"Asia/Kolkata",
             "current":{"time":"2026-10-18T10:00","temperature_2m":29.4,"relative_humidity_2m":78,"precipitation":0.0,"weather_code":3,"wind_speed_10m":12.1},
             "hourly":{"time":["2026-10-18T00:00","2026-10-18T01:00"],"temperature_2m":[27.0,null],"precipitation":[0.2,1.5],
                       "precipitation_probability":[30,null],"wind_speed_10m":[8.0,9.0],"weather_code":[3,61]},
             "daily":{"time":["2026-10-18","2026-10-19"],"weather_code":[61,95],"temperature_2m_max":[31.0,30.0],"temperature_2m_min":[25.0,null],
                      "precipitation_sum":[12.5,null],"precipitation_probability_max":[80,90],"wind_speed_10m_max":[20.0,25.0]}}
            """;

    @Test
    void parsesForecastAndKeepsMissingValuesNull() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withSuccess(FORECAST_JSON, MediaType.APPLICATION_JSON));
        ForecastResponse f = provider.fetchForecast(chennai);
        assertEquals("Asia/Kolkata", f.location().timezone());
        assertEquals(2, f.daily().size());
        assertEquals(12.5, f.daily().get(0).precipitationSumMm());
        assertNull(f.daily().get(1).precipitationSumMm());          // null stays null, not 0
        assertNull(f.daily().get(1).temperatureMinC());
        assertNull(f.hourly().get(1).temperatureC());
        assertEquals(29.4, f.current().temperatureC());
        assertFalse(f.stale());
        assertEquals(Instant.parse("2026-10-18T05:00:00Z"), f.fetchedAt());
    }

    @Test
    void retriesServerErrorsThenSucceeds() {
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withSuccess(FORECAST_JSON, MediaType.APPLICATION_JSON));
        assertEquals(2, provider.fetchForecast(chennai).daily().size());
        server.verify();
    }

    @Test
    void givesUpAfterMaxAttempts() {
        server.expect(ExpectedCount.times(3), requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withServerError());
        ProviderUnavailableException e = assertThrows(ProviderUnavailableException.class, () -> provider.fetchForecast(chennai));
        assertTrue(e.getMessage().contains("not reachable"));
        server.verify();
    }

    @Test
    void rateLimitIsReportedWithoutRetrying() {
        server.expect(ExpectedCount.once(), requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        ProviderUnavailableException e = assertThrows(ProviderUnavailableException.class, () -> provider.fetchForecast(chennai));
        assertTrue(e.getMessage().contains("limit"));
        server.verify();
    }

    @Test
    void responseWithoutDailyDataIsAnErrorNotAnEmptyForecast() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://om/forecast"))).andRespond(withSuccess("{\"timezone\":\"UTC\"}", MediaType.APPLICATION_JSON));
        assertThrows(ProviderUnavailableException.class, () -> provider.fetchForecast(chennai));
    }

    @Test
    void geocodingWithNoMatchesReturnsEmptyList() {
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://om/geo"))).andRespond(withSuccess("{\"generationtime_ms\":0.5}", MediaType.APPLICATION_JSON));
        assertTrue(provider.geocode("zzzz", 5).isEmpty());
    }

    @Test
    void geocodingParsesResults() {
        String json = "{\"results\":[{\"name\":\"Madurai\",\"latitude\":9.92,\"longitude\":78.12,\"country_code\":\"IN\",\"admin1\":\"Tamil Nadu\",\"admin2\":\"Madurai\",\"timezone\":\"Asia/Kolkata\"}]}";
        server.expect(requestTo(org.hamcrest.Matchers.startsWith("http://om/geo"))).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        List<GeoResult> r = provider.geocode("Madurai", 5);
        assertEquals("Tamil Nadu", r.get(0).admin1());
        assertEquals(9.92, r.get(0).latitude());
    }

    @Test
    void dayWithFewerThan24HourlyPrecipitationValuesIsNotAggregated() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        StringBuilder p = new StringBuilder("[");
        for (int i = 0; i < 24; i++) p.append(i == 5 ? "null" : "1.0").append(i < 23 ? "," : "");
        p.append("]");
        String ones = "[" + "1.0,".repeat(23) + "1.0]";
        var h = mapper.readTree("{\"precipitation\":" + p + ",\"temperature_2m\":" + ones + ",\"relative_humidity_2m\":" + ones
                + ",\"surface_pressure\":" + ones + ",\"wind_speed_10m\":" + ones + "}");
        List<Integer> idx = java.util.stream.IntStream.range(0, 24).boxed().toList();
        assertNull(OpenMeteoProvider.aggregateDay(java.time.LocalDate.of(2026, 10, 17), idx, h));
    }
}
