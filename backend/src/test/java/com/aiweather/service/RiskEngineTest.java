package com.aiweather.service;

import static org.junit.jupiter.api.Assertions.*;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.entity.RiskCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RiskEngineTest {
    private final AppProperties.Risk cfg = new AppProperties.Risk("1.0", 15.6, 64.5, 115.6, 30.0, 40, 50.0);
    private final RiskEngine engine = new RiskEngine(cfg);
    private static final LocalDate D0 = LocalDate.of(2026, 10, 18);

    private static DailyForecast day(int i, Double mm, Integer prob, Double wind) {
        return new DailyForecast(D0.plusDays(i), 61, 31.0, 25.0, mm, prob, wind);
    }

    private static ForecastResponse forecast(List<DailyForecast> days, List<HourlyForecast> hours) {
        return new ForecastResponse(new LocationInfo(13.08, 80.27, "Asia/Kolkata"), "Open-Meteo", "https://open-meteo.com/",
                Instant.parse("2026-10-18T05:00:00Z"), D0, D0.plusDays(days.size() - 1), false, null, days, hours);
    }

    private static List<DailyForecast> week(Double mm, Integer prob) {
        List<DailyForecast> l = new ArrayList<>();
        for (int i = 0; i < 7; i++) l.add(day(i, mm, prob, 10.0));
        return l;
    }

    @ParameterizedTest
    @CsvSource({"0,LOW", "15.5,LOW", "15.6,MODERATE", "64.4,MODERATE", "64.5,HIGH", "115.5,HIGH", "115.6,VERY_HIGH", "300,VERY_HIGH"})
    void dailyThresholdsAreInclusiveAtTheLowerBound(double mm, RiskCategory expected) {
        assertEquals(expected, engine.assess(forecast(week(mm, 90), List.of())).category());
    }

    @Test
    void overallCategoryIsTheMostSeriousDay() {
        List<DailyForecast> days = week(2.0, 80);
        days.set(4, day(4, 70.0, 90, 10.0));
        RiskEngine.Result r = engine.assess(forecast(days, List.of()));
        assertEquals(RiskCategory.HIGH, r.category());
        assertEquals(70.0, r.maxDailyMm());
        assertTrue(r.reasons().get(0).contains("2026-10-22"));
    }

    @Test
    void lowRainChanceCapsHighAtModerateAndSaysSo() {
        RiskEngine.Result r = engine.assess(forecast(week(80.0, 20), List.of()));
        assertEquals(RiskCategory.MODERATE, r.category());
        assertTrue(r.reasons().stream().anyMatch(s -> s.contains("capped at Moderate")));
    }

    @Test
    void highHourlyIntensityLiftsALowDayToHigh() {
        List<HourlyForecast> hours = List.of(new HourlyForecast("2026-10-19T15:00", 28.0, 35.0, 80, 10.0, 95));
        RiskEngine.Result r = engine.assess(forecast(week(10.0, 80), hours));
        assertEquals(RiskCategory.HIGH, r.category());
    }

    @Test
    void missingRainfallIsInsufficientDataNeverLow() {
        RiskEngine.Result r = engine.assess(forecast(week(null, null), List.of()));
        assertEquals(RiskCategory.INSUFFICIENT_DATA, r.category());
        assertNull(r.maxDailyMm());
    }

    @Test
    void partialMissingDataIsReportedAndNotTreatedAsZero() {
        List<DailyForecast> days = week(1.0, 50);
        days.set(2, day(2, null, null, null));
        RiskEngine.Result r = engine.assess(forecast(days, List.of()));
        assertEquals(RiskCategory.LOW, r.category());
        assertEquals(RiskCategory.INSUFFICIENT_DATA, r.perDay().get(2).category());
        assertTrue(r.reasons().stream().anyMatch(s -> s.contains("1 of 7")));
    }

    @Test
    void strongWindIsMentionedButDoesNotChangeTheCategory() {
        List<DailyForecast> days = week(1.0, 50);
        days.set(1, day(1, 1.0, 50, 70.0));
        RiskEngine.Result r = engine.assess(forecast(days, List.of()));
        assertEquals(RiskCategory.LOW, r.category());
        assertTrue(r.reasons().stream().anyMatch(s -> s.contains("Strong wind")));
    }

    @Test
    void thresholdsComeFromConfiguration() {
        RiskEngine strict = new RiskEngine(new AppProperties.Risk("x", 5, 10, 20, 30.0, 40, 50.0));
        assertEquals(RiskCategory.VERY_HIGH, strict.assess(forecast(week(25.0, 90), List.of())).category());
    }
}
