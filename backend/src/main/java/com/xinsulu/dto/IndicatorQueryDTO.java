package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 财务指标查询DTO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "财务指标查询条件")
public class IndicatorQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "企业ID", example = "1")
    private Long enterpriseId;

    @ApiModelProperty(value = "报表ID", example = "1")
    private Long reportId;

    @ApiModelProperty(value = "指标分类", example = "solvency")
    private String category;

    @ApiModelProperty(value = "指标编码列表（多个用逗号分隔）", example = "CURRENT_RATIO,QUICK_RATIO")
    private String indicatorCodes;

    @ApiModelProperty(value = "状态筛选", example = "warning")
    private String status;

    @ApiModelProperty(value = "开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @ApiModelProperty(value = "结束日期", example = "2024-12-31")
    private LocalDate endDate;

    @ApiModelProperty(value = "是否包含趋势数据", example = "true")
    private Boolean includeTrend;

    @ApiModelProperty(value = "趋势期间数（最近N期）", example = "8")
    private Integer trendPeriods;

    // Getter 和 Setter 方法
    public Long getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(Long enterpriseId) { this.enterpriseId = enterpriseId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getIndicatorCodes() { return indicatorCodes; }
    public void setIndicatorCodes(String indicatorCodes) { this.indicatorCodes = indicatorCodes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Boolean getIncludeTrend() { return includeTrend; }
    public void setIncludeTrend(Boolean includeTrend) { this.includeTrend = includeTrend; }
    public Integer getTrendPeriods() { return trendPeriods; }
    public void setTrendPeriods(Integer trendPeriods) { this.trendPeriods = trendPeriods; }
}
