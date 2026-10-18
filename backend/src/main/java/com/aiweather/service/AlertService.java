package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.dto.ApiDtos.*;
import com.aiweather.entity.*;
import com.aiweather.exception.ApiException;
import com.aiweather.repository.WeatherAlertRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    static final String DISCLAIMER = "Official warnings shown here are entered by administrators and may be out of date. "
            + "System estimates are screening results from forecast data. Neither replaces official IMD warnings or local authority instructions.";

    private final WeatherAlertRepository repo;
    private final RiskService risk;
    private final AppProperties props;
    private final Clock clock;

    public AlertService(WeatherAlertRepository repo, RiskService risk, AppProperties props, Clock clock) {
        this.repo = repo;
        this.risk = risk;
        this.props = props;
        this.clock = clock;
    }

    // -------------------------------------------------------------- public view
    public AlertsResponse forLocation(Coordinates c) {
        Instant now = clock.instant();
        List<AlertDto> official = repo.findActive(now).stream()
                .filter(a -> a.getOrigin() == AlertOrigin.ADMIN_ENTERED_OFFICIAL)
                .filter(a -> coversPoint(a, c))
                .map(AlertService::toDto)
                .sorted(java.util.Comparator.comparing((AlertDto a) -> a.severity().ordinal()).reversed())
                .toList();
        List<AlertDto> system = new ArrayList<>();
        String note = null;
        try {
            system.addAll(systemEstimates(risk.assess(c), now));
        } catch (ApiException e) {
            note = "System estimates are unavailable because the forecast could not be loaded: " + e.getMessage();
        }
        return new AlertsResponse(official, system, note, props.alerts().officialLinks(), DISCLAIMER);
    }

    /** Alerts without a mapped area apply to everyone (their affectedAreas text is shown); mapped ones only inside their radius. */
    static boolean coversPoint(WeatherAlert a, Coordinates c) {
        if (a.getCenterLatitude() == null) return true;
        return haversineKm(a.getCenterLatitude().doubleValue(), a.getCenterLongitude().doubleValue(), c.latitude(), c.longitude())
                <= a.getRadiusKm().doubleValue();
    }

    static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    /** Computed on request (nothing stored), so the same event can never be listed twice. */
    List<AlertDto> systemEstimates(RiskResponse r, Instant now) {
        if (r.category() != RiskCategory.HIGH && r.category() != RiskCategory.VERY_HIGH) return List.of();
        AlertSeverity sev = r.category() == RiskCategory.VERY_HIGH ? AlertSeverity.RED : AlertSeverity.ORANGE;
        String label = r.category() == RiskCategory.VERY_HIGH ? "Very high" : "High";
        String title = String.format(Locale.ROOT, "%s heavy-rain risk in the forecast (screening estimate)", label);
        String msg = String.join("\n", r.reasons()) + "\nThis is an automatic estimate from forecast data, not an official warning.";
        return List.of(new AlertDto(null, AlertOrigin.SYSTEM_GENERATED, title, msg, sev, "Rule-based screening v" + r.methodVersion(),
                "https://open-meteo.com/", now, r.windowStart().atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                r.windowEnd().plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(), "Selected location", null, null, null, false));
    }

    // -------------------------------------------------------------- admin
    public List<AlertDto> listAll() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(AlertService::toDto).toList();
    }

    @Transactional
    public AlertDto create(AlertRequest req, String admin) {
        WeatherAlert a = new WeatherAlert();
        apply(a, req);
        a.setCreatedBy(admin);
        if (repo.existsByDedupeKey(a.getDedupeKey())) {
            throw new ApiException(HttpStatus.CONFLICT, "duplicate_alert", "An alert with the same source, areas and validity period already exists.");
        }
        log.info("Admin '{}' created alert '{}'", admin, a.getTitle());
        return toDto(repo.save(a));
    }

    @Transactional
    public AlertDto update(Long id, AlertRequest req, String admin) {
        WeatherAlert a = repo.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not_found", "Alert " + id + " does not exist."));
        apply(a, req);
        if (repo.existsByDedupeKeyAndIdNot(a.getDedupeKey(), id)) {
            throw new ApiException(HttpStatus.CONFLICT, "duplicate_alert", "Another alert with the same source, areas and validity period already exists.");
        }
        log.info("Admin '{}' updated alert {}", admin, id);
        return toDto(repo.save(a));
    }

    @Transactional
    public void delete(Long id, String admin) {
        if (!repo.existsById(id)) throw new ApiException(HttpStatus.NOT_FOUND, "not_found", "Alert " + id + " does not exist.");
        repo.deleteById(id);
        log.info("Admin '{}' deleted alert {}", admin, id);
    }

    void apply(WeatherAlert a, AlertRequest r) {
        if (!r.validTo().isAfter(r.validFrom())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "validTo must be after validFrom.");
        }
        boolean anyGeo = r.centerLatitude() != null || r.centerLongitude() != null || r.radiusKm() != null;
        if (anyGeo) {
            if (r.centerLatitude() == null || r.centerLongitude() == null || r.radiusKm() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "Give all of centerLatitude, centerLongitude and radiusKm, or none.");
            }
            Coordinates.of(r.centerLatitude(), r.centerLongitude()); // range check
            if (r.radiusKm() <= 0 || r.radiusKm() > 2000) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "invalid_input", "radiusKm must be between 0 and 2000.");
            }
        }
        a.setOrigin(AlertOrigin.ADMIN_ENTERED_OFFICIAL);
        a.setTitle(r.title().trim());
        a.setMessage(r.message().trim());
        a.setSeverity(r.severity());
        a.setSourceName(r.sourceName().trim());
        a.setSourceUrl(r.sourceUrl().trim());
        a.setIssuedAt(r.issuedAt());
        a.setValidFrom(r.validFrom());
        a.setValidTo(r.validTo());
        a.setAffectedAreas(r.affectedAreas().trim());
        a.setCenterLatitude(anyGeo ? BigDecimal.valueOf(r.centerLatitude()) : null);
        a.setCenterLongitude(anyGeo ? BigDecimal.valueOf(r.centerLongitude()) : null);
        a.setRadiusKm(anyGeo ? BigDecimal.valueOf(r.radiusKm()) : null);
        a.setTest(r.isTest());
        a.setDedupeKey(dedupeKey(a));
    }

    static String dedupeKey(WeatherAlert a) {
        String raw = String.join("|", a.getOrigin().name(), a.getSourceUrl(), a.getAffectedAreas().toLowerCase(Locale.ROOT),
                a.getValidFrom().toString(), a.getValidTo().toString(), String.valueOf(a.isTest()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static AlertDto toDto(WeatherAlert a) {
        return new AlertDto(a.getId(), a.getOrigin(), a.getTitle(), a.getMessage(), a.getSeverity(), a.getSourceName(), a.getSourceUrl(),
                a.getIssuedAt(), a.getValidFrom(), a.getValidTo(), a.getAffectedAreas(),
                a.getCenterLatitude() == null ? null : a.getCenterLatitude().doubleValue(),
                a.getCenterLongitude() == null ? null : a.getCenterLongitude().doubleValue(),
                a.getRadiusKm() == null ? null : a.getRadiusKm().doubleValue(), a.isTest());
    }
}
