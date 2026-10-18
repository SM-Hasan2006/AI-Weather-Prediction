package com.aiweather.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weather_forecast_daily")
@Getter @Setter @NoArgsConstructor
public class ForecastDaily {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id")
    private ForecastSnapshot snapshot;
    @Column(nullable = false)
    private LocalDate forecastDate;
    private BigDecimal precipitationSumMm;
    private Integer precipitationProbabilityMax;
    private BigDecimal temperatureMaxC;
    private BigDecimal temperatureMinC;
    private BigDecimal windSpeedMaxKmh;
}
