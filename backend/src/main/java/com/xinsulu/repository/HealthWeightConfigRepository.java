package com.xinsulu.repository;

import com.xinsulu.entity.HealthWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HealthWeightConfigRepository extends JpaRepository<HealthWeightConfig, Long> {
    List<HealthWeightConfig> findAllByOrderByIdAsc();
    Optional<HealthWeightConfig> findByDimensionCode(String dimensionCode);
}
