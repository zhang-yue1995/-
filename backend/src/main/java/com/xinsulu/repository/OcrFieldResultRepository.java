package com.xinsulu.repository;

import com.xinsulu.entity.OcrFieldResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * OCR字段识别结果数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface OcrFieldResultRepository extends JpaRepository<OcrFieldResult, Long> {

    /**
     * 根据任务ID查询所有字段识别结果
     *
     * @param taskId 任务ID
     * @return 字段结果列表
     */
    List<OcrFieldResult> findByOcrTaskIdOrderByFieldName(Long taskId);

    List<OcrFieldResult> findByOcrTaskIdOrderByFieldCodeAsc(Long taskId);

    /**
     * 根据任务ID和置信度级别查询
     *
     * @param taskId          任务ID
     * @param confidenceLevel 置信度级别
     * @return 字段结果列表
     */
    List<OcrFieldResult> findByOcrTaskIdAndConfidenceLevel(Long taskId, String confidenceLevel);

    /**
     * 统计任务中低置信度字段数量（置信度低于阈值）
     *
     * @param taskId 任务ID
     * @param maxConfidence 最大置信度值
     * @return 低置信度字段数量
     */
    long countByOcrTaskIdAndConfidenceScoreLessThan(Long taskId, java.math.BigDecimal maxConfidence);

    long countByConfidenceLevelAndIsReviewedAndDeleted(
            String confidenceLevel, Integer isReviewed, Integer deleted);

    /**
     * 根据任务ID和是否已复核查询
     *
     * @param taskId  任务ID
     * @param reviewed 是否已复核
     * @return 字段结果列表
     */
    List<OcrFieldResult> findByOcrTaskIdAndIsReviewed(Long taskId, Integer reviewed);
}
