package com.xinsulu.repository;

import com.xinsulu.entity.IndicatorRuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface IndicatorRuleConfigRepository extends JpaRepository<IndicatorRuleConfig, Long>,
        JpaSpecificationExecutor<IndicatorRuleConfig> {
    Optional<IndicatorRuleConfig> findFirstByIndicatorCodeAndDeleted(String indicatorCode, Integer deleted);
    long countByDeleted(Integer deleted);
}
