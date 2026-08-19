package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务指标值实体类
 * 存储具体报表的指标计算结果
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "financial_indicator_value")
public class FinancialIndicatorValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private FinancialReportArchive report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 100)
    private String indicatorName;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "indicator_value", precision = 18, scale = 4)
    private BigDecimal value;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "status", length = 20)
    private String status;            // normal/warning/danger/not_calculable

    @Column(name = "threshold_min", precision = 10, scale = 4)
    private Double thresholdMin;

    @Column(name = "threshold_max", precision = 10, scale = 4)
    private Double thresholdMax;

    @Column(name = "calculation_detail", length = 1000)
    private String calculationDetail; // 计算过程详情

    @Column(name = "calculated_time")
    private LocalDateTime calculatedTime;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
