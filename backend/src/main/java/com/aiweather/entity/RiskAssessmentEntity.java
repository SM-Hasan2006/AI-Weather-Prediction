package com.aiweather.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "risk_assessments")
@Getter @Setter @NoArgsConstructor
public class RiskAssessmentEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private LocationEntity location;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id")
    private ForecastSnapshot snapshot;
    @Column(nullable = false)
    private String method;
    @Column(nullable = false)
    private String methodVersion;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskCategory category;
    @Column(nullable = false)
    private LocalDate windowStart;
    @Column(nullable = false)
    private LocalDate windowEnd;
    private BigDecimal maxDailyPrecipMm;
    private BigDecimal maxHourlyPrecipMm;
    private Integer maxPrecipProbability;
    @Column(name = "reasons_json", nullable = false)
    private String reasonsJson;
    @Column(name = "thresholds_json", nullable = false)
    private String thresholdsJson;
    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
