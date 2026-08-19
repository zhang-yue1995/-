package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报表归档DTO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "报表归档信息")
public class ReportArchiveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "归档ID")
    private Long id;

    @ApiModelProperty(value = "上传文件ID")
    private Long fileId;

    @ApiModelProperty(value = "OCR任务ID")
    private Long ocrTaskId;

    @NotNull(message = "企业ID不能为空")
    @ApiModelProperty(value = "企业ID", required = true)
    private Long enterpriseId;

    @NotBlank(message = "报告期不能为空")
    @ApiModelProperty(value = "报告期（如：2024年第3季度）", required = true, example = "2024年第3季度")
    private String reportPeriod;

    @NotNull(message = "报告日期不能为空")
    @ApiModelProperty(value = "报告日期", required = true, example = "2024-09-30")
    private LocalDate reportDate;

    @NotBlank(message = "报表类型不能为空")
    @ApiModelProperty(value = "报表类型", required = true, example = "QUARTERLY")
    private String reportType;

    @ApiModelProperty(value = "年度", example = "2024")
    private Integer year;

    @ApiModelProperty(value = "季度（1-4）", example = "3")
    private Integer quarter;

    @ApiModelProperty(value = "月份（1-12）", example = "9")
    private Integer month;

    @ApiModelProperty(value = "数据来源", example = "OCR_AUTO")
    private String dataSource;

    @ApiModelProperty(value = "数据质量评分（0-100）", example = "95.5")
    private BigDecimal dataQualityScore;

    @ApiModelProperty(value = "校验状态", example = "PASSED")
    private String validationStatus;

    @ApiModelProperty(value = "填报状态", example = "DRAFT")
    private String filingStatus;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "客户经理名称")
    private String managerName;

    // Getter 和 Setter 方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
    public Long getOcrTaskId() { return ocrTaskId; }
    public void setOcrTaskId(Long ocrTaskId) { this.ocrTaskId = ocrTaskId; }
    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getQuarter() { return quarter; }
    public void setQuarter(Integer quarter) { this.quarter = quarter; }
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }
    public String getDataSource() { return dataSource; }
    public void setDataSource(String dataSource) { this.dataSource = dataSource; }
    public BigDecimal getDataQualityScore() { return dataQualityScore; }
    public void setDataQualityScore(BigDecimal dataQualityScore) { this.dataQualityScore = dataQualityScore; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getFilingStatus() { return filingStatus; }
    public void setFilingStatus(String filingStatus) { this.filingStatus = filingStatus; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
}
