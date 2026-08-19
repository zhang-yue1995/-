package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 财务指标VO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "财务指标信息")
public class IndicatorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "指标编码")
    private String indicatorCode;

    @ApiModelProperty(value = "指标名称")
    private String indicatorName;

    @ApiModelProperty(value = "分类")
    private String category;

    @ApiModelProperty(value = "维度")
    private String dimension;

    @ApiModelProperty(value = "当前值")
    private BigDecimal value;

    @ApiModelProperty(value = "单位")
    private String unit;

    @ApiModelProperty(value = "状态：normal/warning/danger/not_calculable")
    private String status;

    @ApiModelProperty(value = "正常范围最小值")
    private Double thresholdMin;

    @ApiModelProperty(value = "正常范围最大值")
    private Double thresholdMax;

    @ApiModelProperty(value = "计算详情")
    private String calculationDetail;

    @ApiModelProperty(value = "上期值（用于对比）")
    private BigDecimal previousValue;

    @ApiModelProperty(value = "环比变动率(%)")
    private BigDecimal changeRate;

    @ApiModelProperty(value = "趋势方向：up/down/stable")
    private String trendDirection;

    // Getter 和 Setter
    public String getIndicatorCode() { return indicatorCode; }
    public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
    public String getIndicatorName() { return indicatorName; }
    public void setIndicatorName(String indicatorName) { this.indicatorName = indicatorName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getThresholdMin() { return thresholdMin; }
    public void setThresholdMin(Double thresholdMin) { this.thresholdMin = thresholdMin; }
    public Double getThresholdMax() { return thresholdMax; }
    public void setThresholdMax(Double thresholdMax) { this.thresholdMax = thresholdMax; }
    public String getCalculationDetail() { return calculationDetail; }
    public void setCalculationDetail(String calculationDetail) { this.calculationDetail = calculationDetail; }
    public BigDecimal getPreviousValue() { return previousValue; }
    public void setPreviousValue(BigDecimal previousValue) { this.previousValue = previousValue; }
    public BigDecimal getChangeRate() { return changeRate; }
    public void setChangeRate(BigDecimal changeRate) { this.changeRate = changeRate; }
    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
}
