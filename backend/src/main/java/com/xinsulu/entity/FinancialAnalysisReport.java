package com.xinsulu.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Financial analysis report persisted for preview, approval and PDF export.
 */
@Data
@Entity
@Table(name = "financial_analysis_report")
public class FinancialAnalysisReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private FinancialReportArchive report;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id")
    private Enterprise enterprise;

    @Column(name = "report_title", length = 200)
    private String reportTitle;

    @Column(name = "report_type", length = 50)
    private String reportType;

    @Column(name = "executive_summary", columnDefinition = "TEXT")
    private String executiveSummary;

    @Column(name = "overall_assessment", columnDefinition = "TEXT")
    private String overallAssessment;

    @Column(name = "key_findings", columnDefinition = "TEXT")
    private String keyFindings;

    @Column(name = "risk_analysis", columnDefinition = "TEXT")
    private String riskAnalysis;

    @Column(name = "positive_factors", columnDefinition = "TEXT")
    private String positiveFactors;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions;

    @Column(name = "data_quality_notes", columnDefinition = "TEXT")
    private String dataQualityNotes;

    @Column(name = "generation_method", length = 50)
    private String generationMethod;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "version")
    private Integer version;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "submitted_by", length = 50)
    private String submittedBy;

    @Column(name = "submitted_time")
    private LocalDateTime submittedTime;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "approved_time")
    private LocalDateTime approvedTime;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(name = "deleted", nullable = false)
    private Integer deleted;
}
