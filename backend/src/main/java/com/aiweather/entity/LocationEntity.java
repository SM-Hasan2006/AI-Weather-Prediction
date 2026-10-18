package com.aiweather.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "locations")
@Getter @Setter @NoArgsConstructor
public class LocationEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String admin1;
    private String admin2;
    private String countryCode;
    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal latitude;
    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal longitude;
    private String timezone;
    @Column(insertable = false, updatable = false)
    private Instant createdAt;
}
