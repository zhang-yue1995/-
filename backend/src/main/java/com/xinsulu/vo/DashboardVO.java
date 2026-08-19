package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 工作台统计VO（Dashboard）
 *
 * @author xinsulu-team
 */
@ApiModel(description = "工作台统计数据")
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 概览统计 ==========
    @ApiModelProperty(value = "企业总数")
    private Long totalEnterprises;

    @ApiModelProperty(value = "本月新增企业数")
    private Long newEnterprisesThisMonth;

    @ApiModelProperty(value = "报表总数")
    private Long totalReports;

    @ApiModelProperty(value = "本月新增报表数")
    private Long newReportsThisMonth;

    @ApiModelProperty(value = "本月新增报表数（别名）")
    private Long monthlyNewReports;

    @ApiModelProperty(value = "待处理任务数")
    private Long pendingTasks;

    @ApiModelProperty(value = "待复核数量")
    private Long pendingReview;

    @ApiModelProperty(value = "OCR任务总数")
    private Long totalOcrTasks;

    // ========== 健康评分分布 ==========
    @ApiModelProperty(value = "优秀企业数量（评分>=80）")
    private Long excellentCount;

    @ApiModelProperty(value = "良好企业数量（60<=评分<80）")
    private Long goodCount;

    @ApiModelProperty(value = "一般企业数量（40<=评分<60）")
    private Long fairCount;

    @ApiModelProperty(value = "较差企业数量（评分<40）")
    private Long poorCount;

    @ApiModelProperty(value = "高风险企业数")
    private Long highRiskEnterprises;

    // ========== 最近活动 ==========
    @ApiModelProperty(value = "最近上传的文件列表")
    private List<Map<String, Object>> recentFiles;

    @ApiModelProperty(value = "最近处理的OCR任务列表")
    private List<Map<String, Object>> recentOcrTasks;

    @ApiModelProperty(value = "最近生成的分析报告列表")
    private List<Map<String, Object>> recentReports;

    // ========== 预警信息 ==========
    @ApiModelProperty(value = "高风险预警数量")
    private Long highRiskWarnings;

    @ApiModelProperty(value = "中风险预警数量")
    private Long mediumRiskWarnings;

    @ApiModelProperty(value = "低风险预警数量")
    private Long lowRiskWarnings;

    @ApiModelProperty(value = "预警详情列表")
    private List<Map<String, Object>> warningDetails;

    // ========== 系统状态 ==========
    @ApiModelProperty(value = "系统存储使用量（字节）")
    private Long storageUsed;

    @ApiModelProperty(value = "系统存储总量（字节）")
    private Long storageTotal;

    @ApiModelProperty(value = "存储使用率(%)")
    private BigDecimal storageUsageRate;

    @ApiModelProperty(value = "平均健康评分")
    private BigDecimal averageHealthScore;

    @ApiModelProperty(value = "OCR完成率(%)")
    private BigDecimal ocrCompletionRate;

    // Getter 和 Setter
    public Long getTotalEnterprises() { return totalEnterprises; }
    public void setTotalEnterprises(Long totalEnterprises) { this.totalEnterprises = totalEnterprises; }
    public Long getNewEnterprisesThisMonth() { return newEnterprisesThisMonth; }
    public void setNewEnterprisesThisMonth(Long newEnterprisesThisMonth) { this.newEnterprisesThisMonth = newEnterprisesThisMonth; }
    public Long getTotalReports() { return totalReports; }
    public void setTotalReports(Long totalReports) { this.totalReports = totalReports; }
    public Long getNewReportsThisMonth() { return newReportsThisMonth; }
    public void setNewReportsThisMonth(Long newReportsThisMonth) { this.newReportsThisMonth = newReportsThisMonth; }
    public Long getPendingTasks() { return pendingTasks; }
    public void setPendingTasks(Long pendingTasks) { this.pendingTasks = pendingTasks; }
    public Long getTotalOcrTasks() { return totalOcrTasks; }
    public void setTotalOcrTasks(Long totalOcrTasks) { this.totalOcrTasks = totalOcrTasks; }
    public Long getExcellentCount() { return excellentCount; }
    public void setExcellentCount(Long excellentCount) { this.excellentCount = excellentCount; }
    public Long getGoodCount() { return goodCount; }
    public void setGoodCount(Long goodCount) { this.goodCount = goodCount; }
    public Long getFairCount() { return fairCount; }
    public void setFairCount(Long fairCount) { this.fairCount = fairCount; }
    public Long getPoorCount() { return poorCount; }
    public void setPoorCount(Long poorCount) { this.poorCount = poorCount; }
    public List<Map<String, Object>> getRecentFiles() { return recentFiles; }
    public void setRecentFiles(List<Map<String, Object>> recentFiles) { this.recentFiles = recentFiles; }
    public List<Map<String, Object>> getRecentOcrTasks() { return recentOcrTasks; }
    public void setRecentOcrTasks(List<Map<String, Object>> recentOcrTasks) { this.recentOcrTasks = recentOcrTasks; }
    public List<Map<String, Object>> getRecentReports() { return recentReports; }
    public void setRecentReports(List<Map<String, Object>> recentReports) { this.recentReports = recentReports; }
    public Long getHighRiskWarnings() { return highRiskWarnings; }
    public void setHighRiskWarnings(Long highRiskWarnings) { this.highRiskWarnings = highRiskWarnings; }
    public Long getMediumRiskWarnings() { return mediumRiskWarnings; }
    public void setMediumRiskWarnings(Long mediumRiskWarnings) { this.mediumRiskWarnings = mediumRiskWarnings; }
    public Long getLowRiskWarnings() { return lowRiskWarnings; }
    public void setLowRiskWarnings(Long lowRiskWarnings) { this.lowRiskWarnings = lowRiskWarnings; }
    public List<Map<String, Object>> getWarningDetails() { return warningDetails; }
    public void setWarningDetails(List<Map<String, Object>> warningDetails) { this.warningDetails = warningDetails; }
    public Long getStorageUsed() { return storageUsed; }
    public void setStorageUsed(Long storageUsed) { this.storageUsed = storageUsed; }
    public Long getStorageTotal() { return storageTotal; }
    public void setStorageTotal(Long storageTotal) { this.storageTotal = storageTotal; }
    public BigDecimal getStorageUsageRate() { return storageUsageRate; }
    public void setStorageUsageRate(BigDecimal storageUsageRate) { this.storageUsageRate = storageUsageRate; }
    public Long getMonthlyNewReports() { return monthlyNewReports; }
    public void setMonthlyNewReports(Long monthlyNewReports) { this.monthlyNewReports = monthlyNewReports; }
    public Long getPendingReview() { return pendingReview; }
    public void setPendingReview(Long pendingReview) { this.pendingReview = pendingReview; }
    public Long getHighRiskEnterprises() { return highRiskEnterprises; }
    public void setHighRiskEnterprises(Long highRiskEnterprises) { this.highRiskEnterprises = highRiskEnterprises; }
    public BigDecimal getAverageHealthScore() { return averageHealthScore; }
    public void setAverageHealthScore(BigDecimal averageHealthScore) { this.averageHealthScore = averageHealthScore; }
    public BigDecimal getOcrCompletionRate() { return ocrCompletionRate; }
    public void setOcrCompletionRate(BigDecimal ocrCompletionRate) { this.ocrCompletionRate = ocrCompletionRate; }
}
