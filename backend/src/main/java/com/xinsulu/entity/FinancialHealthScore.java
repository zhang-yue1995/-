package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务健康评分实体类
 * 存储企业综合健康度评分结果
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "financial_health_score")
public class FinancialHealthScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private FinancialReportArchive report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Column(name = "report_date")
    private java.time.LocalDate reportDate;

    // 综合评分（0-100）
    @Column(name = "total_score", precision = 5, scale = 2)
    private BigDecimal totalScore;

    // 五维评分
    @Column(name = "solvency_score", precision = 5, scale = 2)
    private BigDecimal solvencyScore;      // 偿债能力得分

    @Column(name = "profitability_score", precision = 5, scale = 2)
    private BigDecimal profitabilityScore; // 盈利能力得分

    @Column(name = "cash_flow_score", precision = 5, scale = 2)
    private BigDecimal cashFlowScore;      // 现金流能力得分

    @Column(name = "operation_score", precision = 5, scale = 2)
    private BigDecimal operationScore;     // 运营能力得分

    @Column(name = "growth_score", precision = 5, scale = 2)
    private BigDecimal growthScore;        // 成长能力得分

    // 风险等级
    @Column(name = "risk_level", length = 20)
    private String riskLevel;

    // 权重配置
    @Column(name = "solvency_weight", precision = 3, scale = 2)
    private BigDecimal solvencyWeight;

    @Column(name = "profitability_weight", precision = 3, scale = 2)
    private BigDecimal profitabilityWeight;

    @Column(name = "cash_flow_weight", precision = 3, scale = 2)
    private BigDecimal cashFlowWeight;

    @Column(name = "operation_weight", precision = 3, scale = 2)
    private BigDecimal operationWeight;

    @Column(name = "growth_weight", precision = 3, scale = 2)
    private BigDecimal growthWeight;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
