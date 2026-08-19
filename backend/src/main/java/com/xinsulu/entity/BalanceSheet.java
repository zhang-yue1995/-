package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产负债表主表实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "balance_sheet")
public class BalanceSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archive_id", nullable = false)
    @JsonIgnore
    private FinancialReportArchive archive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    @JsonIgnore
    private Enterprise enterprise;

    @Column(name = "report_period", nullable = false, length = 20)
    private String reportPeriod;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_current_assets", precision = 18, scale = 2)
    private BigDecimal totalCurrentAssets;

    @Column(name = "total_non_current_assets", precision = 18, scale = 2)
    private BigDecimal totalNonCurrentAssets;

    @Column(name = "total_assets", precision = 18, scale = 2)
    private BigDecimal totalAssets;

    @Column(name = "total_current_liabilities", precision = 18, scale = 2)
    private BigDecimal totalCurrentLiabilities;

    @Column(name = "total_non_current_liabilities", precision = 18, scale = 2)
    private BigDecimal totalNonCurrentLiabilities;

    @Column(name = "total_liabilities", precision = 18, scale = 2)
    private BigDecimal totalLiabilities;

    @Column(name = "total_equity", precision = 18, scale = 2)
    private BigDecimal totalEquity;

    @Column(name = "balance_check_result", length = 20)
    private String balanceCheckResult;

    @Column(name = "balance_difference", precision = 18, scale = 6)
    private BigDecimal balanceDifference;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
