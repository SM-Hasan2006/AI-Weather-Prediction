package com.aiweather.repository;

import com.aiweather.entity.RiskAssessmentEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessmentEntity, Long> {
    Optional<RiskAssessmentEntity> findFirstByLocationIdOrderByCreatedAtDesc(Long locationId);

    @Modifying
    @Query("delete from RiskAssessmentEntity r where r.createdAt < :cutoff")
    int deleteOlderThan(Instant cutoff);
}
