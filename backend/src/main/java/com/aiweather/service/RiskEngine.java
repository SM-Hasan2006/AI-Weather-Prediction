package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.DailyForecast;
import com.aiweather.dto.ApiDtos.DayRisk;
import com.aiweather.dto.ApiDtos.ForecastResponse;
import com.aiweather.dto.ApiDtos.HourlyForecast;
import com.aiweather.entity.RiskCategory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transparent RULE-BASED heavy-rain screening of a forecast. It is NOT a trained model, NOT an official
 * warning, and it does NOT assess flooding (no terrain, drainage or river data).
 *
 * Per day: category from forecast 24 h rainfall (mm) against configurable thresholds;
 *   - an hourly amount >= highHourlyMm lifts the day to at least HIGH;
 *   - a rain chance below minProbabilityPct caps a HIGH/VERY_HIGH day at MODERATE (reason is stated);
 *   - a day with no rainfall value is INSUFFICIENT_DATA and is never counted as LOW.
 * Overall: the most serious day; INSUFFICIENT_DATA only if no day could be assessed.
 */
public class RiskEngine {

    public record Result(RiskCategory category, Double maxDailyMm, List<String> reasons, List<DayRisk> perDay) {}

    private final AppProperties.Risk cfg;

    public RiskEngine(AppProperties.Risk cfg) {
        this.cfg = cfg;
    }

    RiskCategory byAmount(double mm) {
        if (mm >= cfg.veryHighDailyMm()) return RiskCategory.VERY_HIGH;
        if (mm >= cfg.highDailyMm()) return RiskCategory.HIGH;
        if (mm >= cfg.moderateDailyMm()) return RiskCategory.MODERATE;
        return RiskCategory.LOW;
    }

    private static String mm(double v) {
        return String.format(Locale.ROOT, "%.1f mm", v);
    }

    private static String label(RiskCategory c) {
        return switch (c) {
            case LOW -> "Low";
            case MODERATE -> "Moderate";
            case HIGH -> "High";
            case VERY_HIGH -> "Very high";
            case INSUFFICIENT_DATA -> "Insufficient data";
        };
    }

    public Result assess(ForecastResponse f) {
        Map<LocalDate, Double> maxHourly = new HashMap<>();
        if (f.hourly() != null) {
            for (HourlyForecast h : f.hourly()) {
                if (h.precipitationMm() == null) continue;
                maxHourly.merge(LocalDate.parse(h.time().substring(0, 10)), h.precipitationMm(), Math::max);
            }
        }
        List<DayRisk> perDay = new ArrayList<>();
        List<String> dayReasons = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        RiskCategory overall = null;
        Double maxDaily = null;
        int missing = 0;

        for (DailyForecast d : f.daily()) {
            Double amount = d.precipitationSumMm();
            if (amount == null) {
                missing++;
                perDay.add(new DayRisk(d.date(), RiskCategory.INSUFFICIENT_DATA, null, d.precipitationProbabilityMaxPct()));
                continue;
            }
            RiskCategory cat = byAmount(amount);
            String why = null;
            Double hourly = maxHourly.get(d.date());
            if (hourly != null && hourly >= cfg.highHourlyMm() && cat.rank() < RiskCategory.HIGH.rank()) {
                cat = RiskCategory.HIGH;
                why = String.format(Locale.ROOT, "Peak hourly rainfall of %s on %s reaches the hourly intensity screening threshold (%s).",
                        mm(hourly), d.date(), mm(cfg.highHourlyMm()));
            }
            Integer p = d.precipitationProbabilityMaxPct();
            if (p != null && p < cfg.minProbabilityPct() && cat.rank() > RiskCategory.MODERATE.rank()) {
                why = String.format(Locale.ROOT, "%s of rain forecast on %s would be %s, but the chance of rain is only %d%% (below %d%%), so it is capped at Moderate.",
                        mm(amount), d.date(), label(cat), p, cfg.minProbabilityPct());
                cat = RiskCategory.MODERATE;
            }
            if (why == null && cat.rank() >= RiskCategory.MODERATE.rank()) {
                double threshold = switch (cat) {
                    case VERY_HIGH -> cfg.veryHighDailyMm();
                    case HIGH -> cfg.highDailyMm();
                    default -> cfg.moderateDailyMm();
                };
                why = String.format(Locale.ROOT, "Forecast rainfall of %s on %s reaches the %s screening threshold (%s).",
                        mm(amount), d.date(), label(cat), mm(threshold));
            }
            if (why != null) dayReasons.add(cat.rank() + "|" + why);
            if (d.windSpeedMaxKmh() != null && d.windSpeedMaxKmh() >= cfg.strongWindKmh()) {
                notes.add(String.format(Locale.ROOT, "Strong wind up to %.0f km/h is forecast on %s (this does not change the category).",
                        d.windSpeedMaxKmh(), d.date()));
            }
            perDay.add(new DayRisk(d.date(), cat, amount, p));
            if (overall == null || cat.rank() > overall.rank()) overall = cat;
            if (maxDaily == null || amount > maxDaily) maxDaily = amount;
        }

        List<String> reasons = new ArrayList<>();
        if (overall == null) {
            reasons.add("No daily rainfall values were returned for this period, so no risk category can be given.");
            return new Result(RiskCategory.INSUFFICIENT_DATA, null, reasons, perDay);
        }
        dayReasons.stream().sorted(Comparator.comparing((String s) -> -Integer.parseInt(s.substring(0, s.indexOf('|')))))
                .limit(4).forEach(s -> reasons.add(s.substring(s.indexOf('|') + 1)));
        if (overall == RiskCategory.LOW) {
            reasons.add(String.format(Locale.ROOT, "The highest forecast daily rainfall is %s, below the Moderate screening threshold (%s).",
                    mm(maxDaily), mm(cfg.moderateDailyMm())));
        }
        reasons.addAll(notes.stream().limit(1).toList());
        if (missing > 0) {
            reasons.add(missing + " of " + f.daily().size() + " forecast days had no rainfall value and were not assessed.");
        }
        return new Result(overall, maxDaily, reasons, perDay);
    }
}
