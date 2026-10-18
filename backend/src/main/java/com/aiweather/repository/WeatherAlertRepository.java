package com.aiweather.repository;

import com.aiweather.entity.WeatherAlert;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WeatherAlertRepository extends JpaRepository<WeatherAlert, Long> {
    @Query("select a from WeatherAlert a where a.validTo > :now and a.validFrom <= :now order by a.validFrom desc")
    List<WeatherAlert> findActive(Instant now);

    List<WeatherAlert> findAllByOrderByCreatedAtDesc();

    boolean existsByDedupeKey(String dedupeKey);

    boolean existsByDedupeKeyAndIdNot(String dedupeKey, Long id);
}
