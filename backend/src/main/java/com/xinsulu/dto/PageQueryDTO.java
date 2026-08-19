package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;

/**
 * 通用分页查询DTO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "分页查询参数")
public class PageQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "当前页码（从1开始）", example = "1")
    private Integer pageNum = 1;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer pageSize = 10;

    @ApiModelProperty(value = "排序字段", example = "createdTime")
    private String sortBy;

    @ApiModelProperty(value = "排序方向（asc/desc）", example = "desc")
    private String sortOrder = "desc";

    @ApiModelProperty(value = "关键词搜索")
    private String keyword;

    @ApiModelProperty(value = "风险等级")
    private String riskLevel;

    @ApiModelProperty(value = "报表期间")
    private String period;

    @ApiModelProperty(value = "归档状态")
    private String status;

    @ApiModelProperty(value = "是否只返回至少包含一期有效报表的企业")
    private Boolean activeReportsOnly;

    // Getter 和 Setter 方法
    public Integer getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize != null && pageSize > 0 ? pageSize : 10;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getActiveReportsOnly() { return activeReportsOnly; }
    public void setActiveReportsOnly(Boolean activeReportsOnly) { this.activeReportsOnly = activeReportsOnly; }

    /**
     * 获取偏移量（用于SQL LIMIT）
     *
     * @return 偏移量
     */
    public int getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }
}
