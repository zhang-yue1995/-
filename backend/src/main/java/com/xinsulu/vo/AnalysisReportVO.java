package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分析报告VO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "财务分析报告")
public class AnalysisReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "报告ID")
    private Long reportId;

    @ApiModelProperty(value = "关联的报表归档ID")
    private Long archiveId;

    @ApiModelProperty(value = "企业ID")
    private Long enterpriseId;

    @ApiModelProperty(value = "企业名称")
    private String enterpriseName;

    @ApiModelProperty(value = "报表期")
    private String reportPeriod;

    @ApiModelProperty(value = "报告标题")
    private String reportTitle;

    @ApiModelProperty(value = "报告类型：comprehensive/risk/trend/suggestion")
    private String reportType;

    @ApiModelProperty(value = "执行摘要")
    private String executiveSummary;

    @ApiModelProperty(value = "总体评价")
    private String overallAssessment;

    @ApiModelProperty(value = "主要发现")
    private String keyFindings;

    @ApiModelProperty(value = "风险分析")
    private String riskAnalysis;

    @ApiModelProperty(value = "积极因素")
    private String positiveFactors;

    @ApiModelProperty(value = "改善建议")
    private String improvementSuggestions;

    @ApiModelProperty(value = "数据质量说明")
    private String dataQualityNotes;

    @ApiModelProperty(value = "生成方法：rule_based/ai_model")
    private String generationMethod;

    @ApiModelProperty(value = "版本号")
    private Integer version;

    @ApiModelProperty(value = "状态：draft/generated/approved")
    private String status;

    @ApiModelProperty(value = "提交审批人")
    private String submittedBy;

    @ApiModelProperty(value = "提交审批时间")
    private LocalDateTime submittedTime;

    @ApiModelProperty(value = "审批人")
    private String approvedBy;

    @ApiModelProperty(value = "审批完成时间")
    private LocalDateTime approvedTime;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;

    // Getter 和 Setter
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }
    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public String getEnterpriseName() { return enterpriseName; }
    public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public String getExecutiveSummary() { return executiveSummary; }
    public void setExecutiveSummary(String executiveSummary) { this.executiveSummary = executiveSummary; }
    public String getOverallAssessment() { return overallAssessment; }
    public void setOverallAssessment(String overallAssessment) { this.overallAssessment = overallAssessment; }
    public String getKeyFindings() { return keyFindings; }
    public void setKeyFindings(String keyFindings) { this.keyFindings = keyFindings; }
    public String getRiskAnalysis() { return riskAnalysis; }
    public void setRiskAnalysis(String riskAnalysis) { this.riskAnalysis = riskAnalysis; }
    public String getPositiveFactors() { return positiveFactors; }
    public void setPositiveFactors(String positiveFactors) { this.positiveFactors = positiveFactors; }
    public String getImprovementSuggestions() { return improvementSuggestions; }
    public void setImprovementSuggestions(String improvementSuggestions) { this.improvementSuggestions = improvementSuggestions; }
    public String getDataQualityNotes() { return dataQualityNotes; }
    public void setDataQualityNotes(String dataQualityNotes) { this.dataQualityNotes = dataQualityNotes; }
    public String getGenerationMethod() { return generationMethod; }
    public void setGenerationMethod(String generationMethod) { this.generationMethod = generationMethod; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(String submittedBy) { this.submittedBy = submittedBy; }
    public LocalDateTime getSubmittedTime() { return submittedTime; }
    public void setSubmittedTime(LocalDateTime submittedTime) { this.submittedTime = submittedTime; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getApprovedTime() { return approvedTime; }
    public void setApprovedTime(LocalDateTime approvedTime) { this.approvedTime = approvedTime; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
