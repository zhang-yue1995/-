package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表详情VO（包含三大报表数据）
 *
 * @author xinsulu-team
 */
@ApiModel(description = "报表详情")
public class ReportDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // 基本信息
    @ApiModelProperty(value = "归档ID")
    private Long archiveId;

    @ApiModelProperty(value = "企业ID")
    private Long enterpriseId;

    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value = "统一社会信用代码")
    private String enterpriseCreditCode;

    @ApiModelProperty(value = "所属行业")
    private String enterpriseIndustry;

    @ApiModelProperty(value = "报告期")
    private String reportPeriod;

    @ApiModelProperty(value = "报告日期")
    private LocalDate reportDate;

    @ApiModelProperty(value = "报表类型")
    private String reportType;

    @ApiModelProperty(value = "数据来源")
    private String dataSource;

    @ApiModelProperty(value = "校验状态")
    private String validationStatus;

    @ApiModelProperty(value = "数据质量评分")
    private BigDecimal dataQualityScore;

    @ApiModelProperty(value = "填报状态")
    private String filingStatus;

    @ApiModelProperty(value = "客户经理名称")
    private String managerName;

    // 资产负债表摘要
    @ApiModelProperty(value = "资产总计")
    private BigDecimal totalAssets;

    @ApiModelProperty(value = "负债合计")
    private BigDecimal totalLiabilities;

    @ApiModelProperty(value = "所有者权益合计")
    private BigDecimal totalEquity;

    @ApiModelProperty(value = "资产负债表平衡差额")
    private BigDecimal balanceDifference;

    // 利润表摘要
    @ApiModelProperty(value = "营业总收入")
    private BigDecimal totalRevenue;

    @ApiModelProperty(value = "营业总成本")
    private BigDecimal totalCost;

    @ApiModelProperty(value = "净利润")
    private BigDecimal netProfit;

    @ApiModelProperty(value = "毛利率(%)")
    private BigDecimal grossProfitRate;

    // 现金流量表摘要
    @ApiModelProperty(value = "经营活动现金流净额")
    private BigDecimal netOperatingCashFlow;

    @ApiModelProperty(value = "投资活动现金流净额")
    private BigDecimal netInvestingCashFlow;

    @ApiModelProperty(value = "筹资活动现金流净额")
    private BigDecimal netFinancingCashFlow;

    // 关联信息
    @ApiModelProperty(value = "OCR任务ID")
    private Long ocrTaskId;

    @ApiModelProperty(value = "健康评分ID")
    private Long healthScoreId;

    @ApiModelProperty(value = "健康评分")
    private BigDecimal healthScore;

    @ApiModelProperty(value = "风险等级")
    private String riskLevel;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "分析报告审批状态：generated/pending_approval/approved")
    private String approvalStatus;

    @ApiModelProperty(value = "提交审批人")
    private String approvalSubmittedBy;

    @ApiModelProperty(value = "提交审批时间")
    private LocalDateTime approvalSubmittedTime;

    @ApiModelProperty(value = "审批人")
    private String approvedBy;

    @ApiModelProperty(value = "审批完成时间")
    private LocalDateTime approvedTime;

    @ApiModelProperty(value = "审批/驳回意见")
    private String reviewComment;

    // Getter 和 Setter 方法
    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }
    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public String getEnterpriseName() { return enterpriseName; }
    public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
    public String getEnterpriseCreditCode() { return enterpriseCreditCode; }
    public void setEnterpriseCreditCode(String enterpriseCreditCode) { this.enterpriseCreditCode = enterpriseCreditCode; }
    public String getEnterpriseIndustry() { return enterpriseIndustry; }
    public void setEnterpriseIndustry(String enterpriseIndustry) { this.enterpriseIndustry = enterpriseIndustry; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public BigDecimal getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public String getFilingStatus() { return filingStatus; }
    public void setFilingStatus(String filingStatus) { this.filingStatus = filingStatus; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public BigDecimal getTotalAssets() { return totalAssets; }
    public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
    public BigDecimal getTotalLiabilities() { return totalLiabilities; }
    public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
    public BigDecimal getTotalEquity() { return totalEquity; }
    public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
    public BigDecimal getBalanceDifference() { return balanceDifference; }
    public void setBalanceDifference(BigDecimal balanceDifference) { this.balanceDifference = balanceDifference; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getNetProfit() { return netProfit; }
    public void setNetProfit(BigDecimal netProfit) { this.netProfit = netProfit; }
    public BigDecimal getGrossProfitRate() { return grossProfitRate; }
    public void setGrossProfitRate(BigDecimal grossProfitRate) { this.grossProfitRate = grossProfitRate; }
    public BigDecimal getNetOperatingCashFlow() { return netOperatingCashFlow; }
    public void setNetOperatingCashFlow(BigDecimal netOperatingCashFlow) { this.netOperatingCashFlow = netOperatingCashFlow; }
    public BigDecimal getNetInvestingCashFlow() { return netInvestingCashFlow; }
    public void setNetInvestingCashFlow(BigDecimal netInvestingCashFlow) { this.netInvestingCashFlow = netInvestingCashFlow; }
    public BigDecimal getNetFinancingCashFlow() { return netFinancingCashFlow; }
    public void setNetFinancingCashFlow(BigDecimal netFinancingCashFlow) { this.netFinancingCashFlow = netFinancingCashFlow; }
    public Long getOcrTaskId() { return ocrTaskId; }
    public void setOcrTaskId(Long ocrTaskId) { this.ocrTaskId = ocrTaskId; }
    public Long getHealthScoreId() { return healthScoreId; }
    public void setHealthScoreId(Long healthScoreId) { this.healthScoreId = healthScoreId; }
    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal healthScore) { this.healthScore = healthScore; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    public String getApprovalSubmittedBy() { return approvalSubmittedBy; }
    public void setApprovalSubmittedBy(String approvalSubmittedBy) { this.approvalSubmittedBy = approvalSubmittedBy; }
    public LocalDateTime getApprovalSubmittedTime() { return approvalSubmittedTime; }
    public void setApprovalSubmittedTime(LocalDateTime approvalSubmittedTime) { this.approvalSubmittedTime = approvalSubmittedTime; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
}
