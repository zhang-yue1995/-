package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 利润表主表实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "income_statement")
public class IncomeStatement {

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

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_operating_income", precision = 18, scale = 2)
    private BigDecimal totalOperatingIncome;

    @Column(name = "total_operating_cost", precision = 18, scale = 2)
    private BigDecimal totalOperatingCost;

    @Column(name = "operating_profit", precision = 18, scale = 2)
    private BigDecimal operatingProfit;

    @Column(name = "total_profit", precision = 18, scale = 2)
    private BigDecimal totalProfit;

    @Column(name = "net_profit", precision = 18, scale = 2)
    private BigDecimal netProfit;

    @Column(name = "net_profit_deducted", precision = 18, scale = 2)
    private BigDecimal netProfitDeducted;

    @Column(name = "crosscheck_result", length = 20)
    private String crosscheckResult;

    @Column(name = "crosscheck_difference", precision = 18, scale = 6)
    private BigDecimal crosscheckDifference;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
