package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.entity.ForecastSnapshot;
import com.aiweather.entity.LocationEntity;
import com.aiweather.entity.RiskAssessmentEntity;
import com.aiweather.repository.ForecastSnapshotRepository;
import com.aiweather.repository.LocationRepository;
import com.aiweather.repository.RiskAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RiskService {
    private static final Logger log = LoggerFactory.getLogger(RiskService.class);
    public static final String METHOD = "RULE_BASED";

    private final WeatherService weather;
    private final RiskEngine engine;
    private final AppProperties.Risk cfg;
    private final LocationRepository locations;
    private final ForecastSnapshotRepository snapshots;
    private final RiskAssessmentRepository risks;
    private final ObjectMapper mapper;
    private final Clock clock;

    public RiskService(WeatherService weather, AppProperties props, LocationRepository locations, ForecastSnapshotRepository snapshots,
                       RiskAssessmentRepository risks, ObjectMapper mapper, Clock clock) {
        this.weather = weather;
        this.cfg = props.risk();
        this.engine = new RiskEngine(props.risk());
        this.locations = locations;
        this.snapshots = snapshots;
        this.risks = risks;
        this.mapper = mapper;
        this.clock = clock;
    }

    public RiskResponse assess(Coordinates c) {
        ForecastResponse f = weather.forecast(c);
        RiskEngine.Result r = engine.assess(f);
        RiskResponse response = new RiskResponse(r.category(), METHOD, cfg.methodVersion(),
                f.provider() + " forecast", clock.instant(), f.fetchedAt(), f.stale(), f.forecastStart(), f.forecastEnd(),
                r.maxDailyMm(), r.reasons(), r.perDay(), Thresholds.from(cfg), false,
                "Rainfall screening from forecast data only. It does not assess flooding and is not an official warning.");
        try {
            persist(c, f, response);
        } catch (RuntimeException e) {
            log.warn("Could not store risk assessment: {}", e.toString());
        }
        return response;
    }

    /** One stored assessment per fetched forecast (not one per page view). */
    private void persist(Coordinates c, ForecastResponse f, RiskResponse r) {
        Optional<LocationEntity> loc = locations.findByLatitudeAndLongitude(SnapshotService.coord(c.latitude()), SnapshotService.coord(c.longitude()));
        if (loc.isEmpty()) return;
        Optional<RiskAssessmentEntity> last = risks.findFirstByLocationIdOrderByCreatedAtDesc(loc.get().getId());
        if (last.isPresent() && last.get().getCreatedAt() != null && !last.get().getCreatedAt().isBefore(f.fetchedAt())) return;
        RiskAssessmentEntity e = new RiskAssessmentEntity();
        e.setLocation(loc.get());
        snapshots.findFirstByLocationIdOrderByFetchedAtDesc(loc.get().getId()).ifPresent(e::setSnapshot);
        e.setMethod(r.method());
        e.setMethodVersion(r.methodVersion());
        e.setCategory(r.category());
        e.setWindowStart(r.windowStart());
        e.setWindowEnd(r.windowEnd());
        e.setMaxDailyPrecipMm(SnapshotService.dec(r.maxDailyPrecipMm()));
        e.setMaxPrecipProbability(r.perDay().stream().map(DayRisk::probabilityPct).filter(java.util.Objects::nonNull).max(Integer::compare).orElse(null));
        try {
            e.setReasonsJson(mapper.writeValueAsString(r.reasons()));
            e.setThresholdsJson(mapper.writeValueAsString(r.thresholds()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(ex);
        }
        risks.save(e);
    }
}
