package com.aiweather.repository;

import com.aiweather.entity.ForecastSnapshot;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ForecastSnapshotRepository extends JpaRepository<ForecastSnapshot, Long> {
    Optional<ForecastSnapshot> findFirstByLocationIdOrderByFetchedAtDesc(Long locationId);

    @Modifying
    @Query("delete from ForecastSnapshot s where s.fetchedAt < :cutoff")
    int deleteOlderThan(Instant cutoff);
}
