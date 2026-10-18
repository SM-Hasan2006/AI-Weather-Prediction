package com.aiweather.controller;

import com.aiweather.dto.ApiDtos.HealthResponse;
import com.aiweather.service.MlService;
import java.sql.Connection;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final DataSource dataSource;
    private final MlService ml;
    private final Clock clock;

    public HealthController(DataSource dataSource, MlService ml, Clock clock) {
        this.dataSource = dataSource;
        this.ml = ml;
        this.clock = clock;
    }

    /** DB down => overall DOWN. ML service down only degrades (weather features still work). */
    @GetMapping("/health")
    public HealthResponse health() {
        String db;
        try (Connection c = dataSource.getConnection()) {
            db = c.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            db = "DOWN";
        }
        String mlState = ml.reachable() ? "UP" : "DOWN";
        String overall = "DOWN".equals(db) ? "DOWN" : "DOWN".equals(mlState) ? "DEGRADED" : "UP";
        return new HealthResponse(overall, db, mlState, clock.instant());
    }
}
