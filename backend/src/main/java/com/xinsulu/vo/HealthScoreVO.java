package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 健康评分VO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "财务健康评分详情")
public class HealthScoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "评分记录ID")
    private Long scoreId;

    @ApiModelProperty(value = "企业ID")
    private Long enterpriseId;

    @ApiModelProperty(value = "报告日期")
    private LocalDate reportDate;

    // 综合评分
    @ApiModelProperty(value = "综合评分（0-100）")
    private BigDecimal totalScore;

    @ApiModelProperty(value = "风险等级")
    private String riskLevel;

    @ApiModelProperty(value = "风险等级描述")
    private String riskLevelDesc;

    // 五维得分
    @ApiModelProperty(value = "偿债能力得分")
    private BigDecimal solvencyScore;

    @ApiModelProperty(value = "偿债能力权重(%)")
    private BigDecimal solvencyWeight;

    @ApiModelProperty(value = "盈利能力得分")
    private BigDecimal profitabilityScore;

    @ApiModelProperty(value = "盈利能力权重(%)")
    private BigDecimal profitabilityWeight;

    @ApiModelProperty(value = "现金流能力得分")
    private BigDecimal cashFlowScore;

    @ApiModelProperty(value = "现金流能力权重(%)")
    private BigDecimal cashFlowWeight;

    @ApiModelProperty(value = "运营能力得分")
    private BigDecimal operationScore;

    @ApiModelProperty(value = "运营能力权重(%)")
    private BigDecimal operationWeight;

    @ApiModelProperty(value = "成长能力得分")
    private BigDecimal growthScore;

    @ApiModelProperty(value = "成长能力权重(%)")
    private BigDecimal growthWeight;

    // 评价摘要
    @ApiModelProperty(value = "评价摘要")
    private String summary;

    // 历史趋势（最近N期的评分）
    @ApiModelProperty(value = "历史评分趋势")
    private List<ScoreTrendItem> trendList;

    /**
     * 评分趋势项内部类
     */
    public static class ScoreTrendItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private LocalDate reportDate;
        private BigDecimal score;
        private String riskLevel;

        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public BigDecimal getScore() { return score; }
        public void setScore(BigDecimal score) { this.score = score; }
        public String getRiskLevel() { return riskLevel; }
        public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    }

    // Getter 和 Setter
    public Long getScoreId() { return scoreId; }
    public void setScoreId(Long scoreId) { this.scoreId = scoreId; }
    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getRiskLevelDesc() { return riskLevelDesc; }
    public void setRiskLevelDesc(String riskLevelDesc) { this.riskLevelDesc = riskLevelDesc; }
    public BigDecimal getSolvencyScore() { return solvencyScore; }
    public void setSolvencyScore(BigDecimal solvencyScore) { this.solvencyScore = solvencyScore; }
    public BigDecimal getSolvencyWeight() { return solvencyWeight; }
    public void setSolvencyWeight(BigDecimal solvencyWeight) { this.solvencyWeight = solvencyWeight; }
    public BigDecimal getProfitabilityScore() { return profitabilityScore; }
    public void setProfitabilityScore(BigDecimal profitabilityScore) { this.profitabilityScore = profitabilityScore; }
    public BigDecimal getProfitabilityWeight() { return profitabilityWeight; }
    public void setProfitabilityWeight(BigDecimal profitabilityWeight) { this.profitabilityWeight = profitabilityWeight; }
    public BigDecimal getCashFlowScore() { return cashFlowScore; }
    public void setCashFlowScore(BigDecimal cashFlowScore) { this.cashFlowScore = cashFlowScore; }
    public BigDecimal getCashFlowWeight() { return cashFlowWeight; }
    public void setCashFlowWeight(BigDecimal cashFlowWeight) { this.cashFlowWeight = cashFlowWeight; }
    public BigDecimal getOperationScore() { return operationScore; }
    public void setOperationScore(BigDecimal operationScore) { this.operationScore = operationScore; }
    public BigDecimal getOperationWeight() { return operationWeight; }
    public void setOperationWeight(BigDecimal operationWeight) { this.operationWeight = operationWeight; }
    public BigDecimal getGrowthScore() { return growthScore; }
    public void setGrowthScore(BigDecimal growthScore) { this.growthScore = growthScore; }
    public BigDecimal getGrowthWeight() { return growthWeight; }
    public void setGrowthWeight(BigDecimal growthWeight) { this.growthWeight = growthWeight; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<ScoreTrendItem> getTrendList() { return trendList; }
    public void setTrendList(List<ScoreTrendItem> trendList) { this.trendList = trendList; }
}
