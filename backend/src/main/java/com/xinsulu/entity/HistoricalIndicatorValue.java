package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 历史指标值实体类
 * 用于存储历史期间的财务指标数据，支持趋势分析
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "historical_indicator_value")
public class HistoricalIndicatorValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Column(name = "indicator_code", nullable = false, length = 50)
    private String indicatorCode;

    @Column(name = "indicator_name", length = 100)
    private String indicatorName;

    @Column(name = "category", length = 30)
    private String category;

    @Column(name = "report_period", length = 20)
    private String reportPeriod;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @Column(name = "indicator_value", precision = 18, scale = 4)
    private BigDecimal value;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "period_type", length = 10)
    private String periodType;        // quarterly/yearly

    @Column(name = "report_year")
    private Integer year;

    @Column(name = "quarter")
    private Integer quarter;

    @Column(name = "change_rate", precision = 10, scale = 4)
    private BigDecimal changeRate;     // 环比变动率

    @Column(name = "year_on_year_change", precision = 10, scale = 4)
    private BigDecimal yearOnYearChange; // 同比变动率

    @Column(name = "trend_direction", length = 10)
    private String trendDirection;     // up/down/stable

    @Column(name = "source_report_id")
    private Long sourceReportId;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
