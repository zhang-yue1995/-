package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 企业信息DTO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "企业信息")
public class EnterpriseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "企业ID（更新时必填）")
    private Long id;

    @NotBlank(message = "企业名称不能为空")
    @Size(max = 200, message = "企业名称长度不能超过200字符")
    @ApiModelProperty(value = "企业名称", required = true, example = "XX科技有限公司")
    private String name;

    @NotBlank(message = "统一社会信用代码不能为空")
    @Size(max = 50, message = "统一社会信用代码长度不能超过50字符")
    @ApiModelProperty(value = "统一社会信用代码", required = true, example = "91110000XXXXXXXXXX")
    private String creditCode;

    @ApiModelProperty(value = "法定代表人", example = "张三")
    private String legalPerson;

    @ApiModelProperty(value = "注册资本（万元）", example = "1000.00")
    private java.math.BigDecimal registeredCapital;

    @ApiModelProperty(value = "成立日期", example = "2020-01-01")
    private String establishDate;

    @Size(max = 100, message = "联系电话长度不能超过100字符")
    @ApiModelProperty(value = "联系电话", example = "010-12345678")
    private String phone;

    @Size(max = 200, message = "地址长度不能超过200字符")
    @ApiModelProperty(value = "注册地址", example = "北京市海淀区XX路XX号")
    private String address;

    @ApiModelProperty(value = "行业分类", example = "软件和信息技术服务业")
    private String industry;

    @ApiModelProperty(value = "企业规模", example = "中型")
    private String enterpriseScale;

    @ApiModelProperty(value = "备注", example = "高新技术企业")
    private String remark;

    @ApiModelProperty(value = "客户经理名称")
    private String managerName;

    // Getter 和 Setter 方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreditCode() { return creditCode; }
    public void setCreditCode(String creditCode) { this.creditCode = creditCode; }
    public String getLegalPerson() { return legalPerson; }
    public void setLegalPerson(String legalPerson) { this.legalPerson = legalPerson; }
    public java.math.BigDecimal getRegisteredCapital() { return registeredCapital; }
    public void setRegisteredCapital(java.math.BigDecimal registeredCapital) { this.registeredCapital = registeredCapital; }
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
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }
}
