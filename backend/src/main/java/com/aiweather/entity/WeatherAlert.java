package com.aiweather.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weather_alerts")
@Getter @Setter @NoArgsConstructor
public class WeatherAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertOrigin origin;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;
    @Column(nullable = false)
    private String sourceName;
    @Column(nullable = false)
    private String sourceUrl;
    @Column(nullable = false)
    private Instant issuedAt;
    @Column(nullable = false)
    private Instant validFrom;
    @Column(nullable = false)
    private Instant validTo;
    @Column(nullable = false)
    private String affectedAreas;
    private BigDecimal centerLatitude;
    private BigDecimal centerLongitude;
    private BigDecimal radiusKm;
    @Column(name = "is_test", nullable = false)
    private boolean test;
    @Column(nullable = false, length = 64)
    private String dedupeKey;
    @Column(nullable = false)
    private String createdBy;
    @Column(insertable = false, updatable = false)
    private Instant createdAt;
    @Column(insertable = false, updatable = false)
    private Instant updatedAt;
}
