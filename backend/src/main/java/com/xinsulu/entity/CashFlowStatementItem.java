package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 现金流量表明细项实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "cash_flow_statement_item")
public class CashFlowStatementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statement_id", nullable = false)
    @JsonIgnore
    private CashFlowStatement statement;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "item_type", length = 20)
    private String itemType;  // operating/investing/financing

    @Column(name = "row_number")
    private Integer rowNumber;

    @Column(precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "current_period_amount", precision = 18, scale = 2)
    private BigDecimal currentPeriodAmount;

    @Column(name = "previous_period_amount", precision = 18, scale = 2)
    private BigDecimal previousPeriodAmount;

    @Column(name = "monthly_amount", precision = 18, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(name = "is_total_row")
    private Boolean isTotalRow;

    @Column(name = "parent_code", length = 50)
    private String parentCode;

    @Column(name = "confidence_level", length = 20)
    private String confidenceLevel;

    @Column(name = "ocr_source_field", length = 200)
    private String ocrSourceField;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
