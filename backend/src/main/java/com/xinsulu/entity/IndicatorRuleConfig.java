package com.xinsulu.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "indicator_rule_config")
public class IndicatorRuleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String indicatorCode;

    @Column(nullable = false, length = 120)
    private String indicatorName;

    @Column(nullable = false, length = 500)
    private String formula;

    @Column(length = 40)
    private String normalThreshold;

    @Column(length = 40)
    private String attentionThreshold;

    @Column(length = 40)
    private String highRiskThreshold;

    @Column(precision = 18, scale = 6)
    private BigDecimal normalThresholdValue;

    @Column(precision = 18, scale = 6)
    private BigDecimal attentionThresholdValue;

    @Column(length = 20)
    private String thresholdDirection;

    private Integer weight;

    @Column(length = 80)
    private String applicableIndustry;

    private Boolean isEnabled;

    @Column(length = 500)
    private String remark;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
