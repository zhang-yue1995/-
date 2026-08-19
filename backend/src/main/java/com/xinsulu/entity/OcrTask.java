package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * OCR任务实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "ocr_task")
public class OcrTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @JsonIgnore
    private UploadedFile file;

    @Column(name = "task_status", nullable = false, length = 20)
    private String taskStatus;

    @Column(name = "task_type", nullable = false, length = 30)
    private String taskType;

    @Column(length = 30)
    private String provider;

    @Column(name = "total_fields")
    private Integer totalFields;

    @Column(name = "recognized_fields")
    private Integer recognizedFields;

    @Column(name = "high_confidence_count")
    private Integer highConfidenceCount;

    @Column(name = "medium_confidence_count")
    private Integer mediumConfidenceCount;

    @Column(name = "low_confidence_count")
    private Integer lowConfidenceCount;

    @Column(name = "average_confidence", precision = 7, scale = 4)
    private BigDecimal averageConfidence;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "source_enterprise_name", length = 200)
    private String sourceEnterpriseName;

    @Column(name = "source_report_period", length = 20)
    private String sourceReportPeriod;

    @Column(name = "source_report_date")
    private LocalDate sourceReportDate;

    @Column(name = "source_unit", length = 20)
    private String sourceUnit;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
