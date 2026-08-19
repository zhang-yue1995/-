package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务报表归档实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "financial_report_archive")
public class FinancialReportArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    @Column(nullable = false, length = 30)
    private String reportType;

    @Column(nullable = false, length = 20)
    private String reportPeriod;

    @Column(nullable = false)
    private Integer reportYear;

    @Column(name = "report_quarter")
    private Integer reportQuarter;

    @Column(name = "report_month")
    private Integer reportMonth;

    @Column(nullable = false, length = 20)
    private String filingStatus;

    @Column(name = "validation_status", length = 20)
    private String validationStatus;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalAssets;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(precision = 18, scale = 2)
    private BigDecimal totalEquity;

    @Column(precision = 18, scale = 2)
    private BigDecimal revenue;

    @Column(precision = 18, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "operating_cash_flow", precision = 18, scale = 2)
    private BigDecimal operatingCashFlow;

    @Column(name = "data_source", length = 50)
    private String dataSource;

    @Column(name = "data_quality_score", precision = 5, scale = 2)
    private BigDecimal dataQualityScore;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "manager_name", length = 100)
    private String managerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_time")
    private LocalDateTime reviewedTime;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
