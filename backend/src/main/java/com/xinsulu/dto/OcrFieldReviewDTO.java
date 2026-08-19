package com.xinsulu.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * OCR字段复核DTO
 *
 * @author xinsulu-team
 */
@ApiModel(description = "OCR字段复核信息")
public class OcrFieldReviewDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "字段结果ID不能为空")
    @ApiModelProperty(value = "字段结果ID", required = true)
    private Long fieldResultId;

    @ApiModelProperty(value = "原始识别值")
    private String originalValue;

    @ApiModelProperty(value = "修正后的值")
    private String correctedValue;

    @ApiModelProperty(value = "修正后的第二金额列")
    private String correctedSecondaryValue;

    @ApiModelProperty(value = "修正后的第三金额列")
    private String correctedTertiaryValue;

    @ApiModelProperty(value = "是否确认正确")
    private Boolean isConfirmedCorrect;

    @ApiModelProperty(value = "置信度（0-100）", example = "99.5")
    private Double confidence;

    @ApiModelProperty(value = "复核说明", example = "已与原件核对，数据正确")
    private String reviewComment;

    @ApiModelProperty(value = "复核人")
    private String reviewer;

    // Getter 和 Setter 方法
    public Long getFieldResultId() { return fieldResultId; }
    public void setFieldResultId(Long fieldResultId) { this.fieldResultId = fieldResultId; }
    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }
    public String getCorrectedValue() { return correctedValue; }
    public void setCorrectedValue(String correctedValue) { this.correctedValue = correctedValue; }
    public String getCorrectedSecondaryValue() { return correctedSecondaryValue; }
    public void setCorrectedSecondaryValue(String correctedSecondaryValue) { this.correctedSecondaryValue = correctedSecondaryValue; }
    public String getCorrectedTertiaryValue() { return correctedTertiaryValue; }
    public void setCorrectedTertiaryValue(String correctedTertiaryValue) { this.correctedTertiaryValue = correctedTertiaryValue; }
    public Boolean getIsConfirmedCorrect() { return isConfirmedCorrect; }
    public void setIsConfirmedCorrect(Boolean isConfirmedCorrect) { this.isConfirmedCorrect = isConfirmedCorrect; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getReviewComment() { return reviewComment; }
    public void setReviewComment(String reviewComment) { this.reviewComment = reviewComment; }
    public String getReviewer() { return reviewer; }
    public void setReviewer(String reviewer) { this.reviewer = reviewer; }
}
