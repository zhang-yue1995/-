package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 财务指标定义实体类
 * 定义系统中支持的所有财务分析指标
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "financial_indicator")
public class FinancialIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "indicator_code", nullable = false, unique = true, length = 50)
    private String indicatorCode;     // 指标编码，如 CURRENT_RATIO

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName;     // 指标名称，如 流动比率

    @Column(name = "category", nullable = false, length = 30)
    private String category;          // 分类：solvency/profitability/cash_flow/operation/growth

    @Column(name = "formula", length = 500)
    private String formula;           // 计算公式描述

    @Column(name = "unit", length = 20)
    private String unit;              // 单位：%、倍、天等

    @Column(name = "normal_range_min", precision = 10, scale = 4)
    private Double normalRangeMin;    // 正常范围最小值

    @Column(name = "normal_range_max", precision = 10, scale = 4)
    private Double normalRangeMax;    // 正常范围最大值

    @Column(name = "weight", precision = 5, scale = 2)
    private Double weight;            // 在健康评分中的权重

    @Column(name = "dimension", length = 20)
    private String dimension;         // 所属维度：偿债能力/盈利能力等

    @Column(name = "risk_direction", length = 10)
    private String riskDirection;     // 风险方向：high_is_bad/low_is_bad

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
