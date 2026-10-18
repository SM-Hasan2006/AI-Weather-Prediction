package com.aiweather.repository;

import com.aiweather.entity.ForecastDaily;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ForecastDailyRepository extends JpaRepository<ForecastDaily, Long> {
    @Query("select d from ForecastDaily d join fetch d.snapshot s "
            + "where s.location.id = :locationId and d.forecastDate between :from and :to")
    List<ForecastDaily> findForLocationBetween(Long locationId, LocalDate from, LocalDate to);
}
