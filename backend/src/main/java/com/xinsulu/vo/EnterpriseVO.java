package com.xinsulu.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 企业信息VO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "企业详细信息")
public class EnterpriseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "企业ID")
    private Long id;

    @ApiModelProperty(value = "企业名称")
    private String name;

    @ApiModelProperty(value = "统一社会信用代码")
    private String creditCode;

    @ApiModelProperty(value = "法定代表人")
    private String legalPerson;

    @ApiModelProperty(value = "注册资本（万元）")
    private BigDecimal registeredCapital;

    @ApiModelProperty(value = "成立日期")
    private String establishDate;

    @ApiModelProperty(value = "联系电话")
    private String phone;

    @ApiModelProperty(value = "注册地址")
    private String address;

    @ApiModelProperty(value = "行业分类")
    private String industry;

    @ApiModelProperty(value = "企业规模")
    private String enterpriseScale;

    @ApiModelProperty(value = "备注")
    private String remark;

    @ApiModelProperty(value = "报表数量")
    private Long reportCount;

    @ApiModelProperty(value = "最新健康评分")
    private BigDecimal latestHealthScore;

    @ApiModelProperty(value = "最新风险等级")
    private String latestRiskLevel;

    @ApiModelProperty(value = "最新报表日期")
    private LocalDate lastReportDate;

    @ApiModelProperty(value = "最新报表期")
    private String latestReportPeriod;

    @ApiModelProperty(value = "客户经理名称")
    private String managerName;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;

    // Getter 和 Setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getLegalPerson() { return legalPerson; }
    public void setLegalPerson(String legalPerson) { this.legalPerson = legalPerson; }
    public BigDecimal getRegisteredCapital() { return registeredCapital; }
    public void setRegisteredCapital(BigDecimal registeredCapital) { this.registeredCapital = registeredCapital; }
    public String getEstablishDate() { return establishDate; }
    public void setEstablishDate(String establishDate) { this.establishDate = establishDate; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getIndustry() { return industry; }
    public void setIndustry(String industry) { this.industry = industry; }
    public String getEnterpriseScale() { return enterpriseScale; }
    public void setEnterpriseScale(String enterpriseScale) { this.enterpriseScale = enterpriseScale; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Long getReportCount() { return reportCount; }
    public void setReportCount(Long reportCount) { this.reportCount = reportCount; }
    public BigDecimal getLatestHealthScore() { return latestHealthScore; }
    public void setLatestHealthScore(BigDecimal latestHealthScore) { this.latestHealthScore = latestHealthScore; }
    public String getLatestRiskLevel() { return latestRiskLevel; }
    public void setLatestRiskLevel(String latestRiskLevel) { this.latestRiskLevel = latestRiskLevel; }
    public LocalDate getLastReportDate() { return lastReportDate; }
    public void setLastReportDate(LocalDate lastReportDate) { this.lastReportDate = lastReportDate; }
    public String getLatestReportPeriod() { return latestReportPeriod; }
    public void setLatestReportPeriod(String latestReportPeriod) { this.latestReportPeriod = latestReportPeriod; }
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}
