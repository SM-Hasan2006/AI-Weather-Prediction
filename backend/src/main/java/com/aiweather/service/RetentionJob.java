package com.aiweather.service;

import com.aiweather.config.AppProperties;
import com.aiweather.repository.ForecastSnapshotRepository;
import com.aiweather.repository.RiskAssessmentRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Retention rule: forecast snapshots (and their daily rows) and risk assessments older than N days are deleted. */
@Component
public class RetentionJob {
    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);
    private final ForecastSnapshotRepository snapshots;
    private final RiskAssessmentRepository risks;
    private final Clock clock;
    private final int days;

    public RetentionJob(ForecastSnapshotRepository snapshots, RiskAssessmentRepository risks, Clock clock, AppProperties props) {
        this.snapshots = snapshots;
        this.risks = risks;
        this.clock = clock;
        this.days = props.retention().snapshotDays();
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void purge() {
        Instant cutoff = clock.instant().minus(Duration.ofDays(days));
        int r = risks.deleteOlderThan(cutoff);
        int s = snapshots.deleteOlderThan(cutoff);
        log.info("Retention: removed {} risk assessments and {} forecast snapshots older than {} days", r, s, days);
    }
}
