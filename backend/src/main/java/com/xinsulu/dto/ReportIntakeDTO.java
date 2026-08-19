package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 管理端报表采集向导提交对象。
 * 企业资料与报表归档在同一事务内落库，避免用户中止上传时留下空企业。
 */
@ApiModel(description = "管理端企业与报表一体化采集")
public class ReportIntakeDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Valid
    @NotNull(message = "企业资料不能为空")
    @ApiModelProperty(value = "企业资料", required = true)
    private EnterpriseDTO enterprise;

    @NotNull(message = "报表归档资料不能为空")
    @ApiModelProperty(value = "报表归档资料", required = true)
    private ReportArchiveDTO report;

    public EnterpriseDTO getEnterprise() { return enterprise; }
    public void setEnterprise(EnterpriseDTO enterprise) { this.enterprise = enterprise; }
    public ReportArchiveDTO getReport() { return report; }
    public void setReport(ReportArchiveDTO report) { this.report = report; }
}
