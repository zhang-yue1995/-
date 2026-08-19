package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 现金流量表主表实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "cash_flow_statement")
public class CashFlowStatement {

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

    // 经营活动现金流
    @Column(name = "cash_inflows_operating", precision = 18, scale = 2)
    private BigDecimal cashInflowsOperating;

    @Column(name = "cash_outflows_operating", precision = 18, scale = 2)
    private BigDecimal cashOutflowsOperating;

    @Column(name = "net_cash_flow_operating", precision = 18, scale = 2)
    private BigDecimal netCashFlowOperating;

    // 投资活动现金流
    @Column(name = "cash_inflows_investing", precision = 18, scale = 2)
    private BigDecimal cashInflowsInvesting;

    @Column(name = "cash_outflows_investing", precision = 18, scale = 2)
    private BigDecimal cashOutflowsInvesting;

    @Column(name = "net_cash_flow_investing", precision = 18, scale = 2)
    private BigDecimal netCashFlowInvesting;

    // 筹资活动现金流
    @Column(name = "cash_inflows_financing", precision = 18, scale = 2)
    private BigDecimal cashInflowsFinancing;

    @Column(name = "cash_outflows_financing", precision = 18, scale = 2)
    private BigDecimal cashOutflowsFinancing;

    @Column(name = "net_cash_flow_financing", precision = 18, scale = 2)
    private BigDecimal netCashFlowFinancing;

    // 汇率变动影响及现金净增加额
    @Column(name = "exchange_rate_effect", precision = 18, scale = 2)
    private BigDecimal exchangeRateEffect;

    @Column(name = "net_increase_in_cash", precision = 18, scale = 2)
    private BigDecimal netIncreaseInCash;

    @Column(name = "cash_at_beginning", precision = 18, scale = 2)
    private BigDecimal cashAtBeginning;

    @Column(name = "cash_at_end", precision = 18, scale = 2)
    private BigDecimal cashAtEnd;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
