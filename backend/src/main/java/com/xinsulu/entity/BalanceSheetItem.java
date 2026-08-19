package com.xinsulu.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产负债表明细项实体类
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "balance_sheet_item")
public class BalanceSheetItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "balance_sheet_id", nullable = false)
    @JsonIgnore
    private BalanceSheet balanceSheet;

    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "item_category", nullable = false, length = 30)
    private String itemCategory;

    @Column(name = "ending_balance", precision = 18, scale = 2)
    private BigDecimal endingBalance;

    @Column(name = "beginning_balance", precision = 18, scale = 2)
    private BigDecimal beginningBalance;

    @Column(name = "change_amount", precision = 18, scale = 2)
    private BigDecimal changeAmount;

    @Column(name = "change_percentage", precision = 10, scale = 4)
    private BigDecimal changePercentage;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "parent_item_code", length = 50)
    private String parentItemCode;

    @Column(name = "is_total_row")
    private Integer isTotalRow;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
