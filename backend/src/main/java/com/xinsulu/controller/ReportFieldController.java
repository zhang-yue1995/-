package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.dto.OcrFieldReviewDTO;
import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.repository.OcrFieldResultRepository;
import com.xinsulu.service.FinancialReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表字段控制器
 * 提供OCR识别字段的查询、复核、更新等功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/report-fields")
@Api(tags = "报表字段管理")
public class ReportFieldController {

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 获取报表的所有识别字段
     *
     * @param reportId 报表ID（OCR任务ID）
     * @return 字段结果列表
     */
    @GetMapping("/report/{reportId}")
    @ApiOperation(value = "获取报表字段", notes = "获取指定报表的所有OCR识别字段及置信度")
    public ApiResponse<List<OcrFieldResult>> getFieldsByReport(@PathVariable Long reportId) {
        log.info("获取报表字段：reportId={}", reportId);
        List<OcrFieldResult> fields = ocrFieldResultRepository.findByOcrTaskIdOrderByFieldName(reportId);
        return ApiResponse.success(fields);
    }

    /**
     * 更新单个字段值
     *
     * @param fieldId  字段ID
     * @param reviewDTO 复核信息
     * @return 操作结果
     */
    @PutMapping("/{fieldId}")
    @ApiOperation(value = "更新单个字段", notes = "修正单个OCR识别字段的值")
    public ApiResponse<Void> updateField(@PathVariable Long fieldId,
                                          @RequestBody OcrFieldReviewDTO reviewDTO) {
        log.info("更新字段：fieldId={}", fieldId);

        OcrFieldResult field = ocrFieldResultRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("字段不存在"));

        // 更新字段信息
        if (reviewDTO.getCorrectedValue() != null) {
            field.setFieldValue(reviewDTO.getCorrectedValue());
            field.setIsReviewed(1);  // 标记为已复核修正
        }
        if (reviewDTO.getConfidence() != null) {
            field.setConfidenceScore(java.math.BigDecimal.valueOf(reviewDTO.getConfidence()));
        }
        if (reviewDTO.getReviewComment() != null) {
            field.setReviewComment(reviewDTO.getReviewComment());
            field.setIsReviewed(1);
        }

        ocrFieldResultRepository.save(field);
        return ApiResponse.success();
    }

    /**
     * 批量更新字段
     *
     * @param reviews 复核结果列表
     * @return 操作结果
     */
    @PutMapping("/batch-update")
    @ApiOperation(value = "批量更新字段", notes = "批量提交多个OCR字段的复核修正结果")
    public ApiResponse<Void> batchUpdate(@RequestBody List<OcrFieldReviewDTO> reviews) {
        log.info("批量更新字段：count={}", reviews.size());

        for (OcrFieldReviewDTO review : reviews) {
            OcrFieldResult field = ocrFieldResultRepository.findById(review.getFieldResultId())
                    .orElse(null);

            if (field != null) {
                if (review.getCorrectedValue() != null) {
                    field.setFieldValue(review.getCorrectedValue());
                    field.setIsReviewed(1);
                }
                if (review.getIsConfirmedCorrect() != null && review.getIsConfirmedCorrect()) {
                    field.setIsReviewed(1);
                }
                field.setReviewComment(review.getReviewComment());
                ocrFieldResultRepository.save(field);
            }
        }

        return ApiResponse.success();
    }

    /**
     * 查看字段原始来源
     * 返回OCR识别的原始图像位置等信息
     *
     * @param fieldId 字段ID
     * @return 字段来源信息
     */
    @GetMapping("/{fieldId}/source")
    @ApiOperation(value = "查看字段来源", notes = "查看OCR识别字段的原始图像位置和来源信息")
    public ApiResponse<Object> getFieldSource(@PathVariable Long fieldId) {
        log.info("查看字段来源：fieldId={}", fieldId);

        OcrFieldResult field = ocrFieldResultRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("字段不存在"));

        // 构建来源信息
        java.util.Map<String, Object> sourceInfo = new java.util.HashMap<>();
        sourceInfo.put("fieldId", field.getId());
        sourceInfo.put("fieldName", field.getFieldName());
        sourceInfo.put("originalValue", field.getFieldValue());
        sourceInfo.put("confidence", field.getConfidenceScore());
        sourceInfo.put("confidenceLevel", field.getConfidenceLevel());
        sourceInfo.put("pageNumber", field.getPageNumber());
        sourceInfo.put("boundingBox", field.getBoundingBox());
        sourceInfo.put("ocrTime", field.getCreatedTime());

        return ApiResponse.success(sourceInfo);
    }
}
