package com.aiweather.repository;

import com.aiweather.entity.LocationEntity;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationEntity, Long> {
    Optional<LocationEntity> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);
}
