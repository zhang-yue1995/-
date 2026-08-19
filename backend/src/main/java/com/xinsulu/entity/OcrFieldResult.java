package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OCR识别字段结果实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "ocr_field_result")
public class OcrFieldResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocr_task_id", nullable = false)
    @JsonIgnore
    private OcrTask ocrTask;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_code", length = 50)
    private String fieldCode;

    @Column(name = "field_value", length = 500)
    private String fieldValue;

    @Column(name = "secondary_value", length = 500)
    private String secondaryValue;

    @Column(name = "tertiary_value", length = 500)
    private String tertiaryValue;

    @Column(name = "numeric_value", precision = 18, scale = 2)
    private BigDecimal numericValue;

    @Column(name = "confidence_score", nullable = false, precision = 7, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @Column(name = "field_type", length = 30)
    private String fieldType;

    @Column(name = "is_reviewed")
    private Integer isReviewed;

    @Column(name = "reviewed_value", precision = 18, scale = 2)
    private BigDecimal reviewedValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @Column(name = "bounding_box", length = 200)
    private String boundingBox;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
