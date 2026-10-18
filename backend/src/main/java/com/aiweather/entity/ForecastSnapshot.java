package com.aiweather.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weather_forecast_snapshots")
@Getter @Setter @NoArgsConstructor
public class ForecastSnapshot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private LocationEntity location;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private Instant fetchedAt;
    @Column(nullable = false)
    private LocalDate forecastStartDate;
    @Column(nullable = false)
    private LocalDate forecastEndDate;
    @Column(nullable = false)
    private String timezone;
    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForecastDaily> days = new ArrayList<>();
}
