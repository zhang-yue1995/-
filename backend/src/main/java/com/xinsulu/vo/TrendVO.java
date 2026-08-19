package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 趋势分析VO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "趋势分析数据")
public class TrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "指标编码")
    private String indicatorCode;

    @ApiModelProperty(value = "指标名称")
    private String indicatorName;

    @ApiModelProperty(value = "分类")
    private String category;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "趋势数据列表")
    private List<TrendDataItem> dataList;

    /**
     * 趋势数据项内部类
     */
    public static class TrendDataItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @ApiModelProperty(value = "报告期")
        private String reportPeriod;

        @ApiModelProperty(value = "报告日期")
        private LocalDate reportDate;

        @ApiModelProperty(value = "指标值")
        private BigDecimal value;

        @ApiModelProperty(value = "环比变动率(%)")
        private BigDecimal changeRate;

        @ApiModelProperty(value = "同比变动率(%)")
        private BigDecimal yearOnYearChange;

        @ApiModelProperty(value = "趋势方向")
        private String trendDirection;

        // Getter 和 Setter
        public String getReportPeriod() { return reportPeriod; }
        public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
        public LocalDate getReportDate() { return reportDate; }
        public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        public BigDecimal getChangeRate() { return changeRate; }
        public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
        public BigDecimal getYearOnYearChange() { return yearOnYearChange; }
        public void setYearOnYearChange(BigDecimal yearOnYearChange) { this.yearOnYearChange = yearOnYearChange; }
        public String getTrendDirection() { return trendDirection; }
        public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
    }

    // Getter 和 Setter
    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public List<TrendDataItem> getDataList() { return dataList; }
    public void setDataList(List<TrendDataItem> dataList) { this.dataList = dataList; }
}
