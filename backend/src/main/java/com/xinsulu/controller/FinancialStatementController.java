package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.service.FinancialReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务报表数据控制器
 * 提供三大财务报表的查询和保存接口
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/financial-statements")
@Api(tags = "三大财务报表")
public class FinancialStatementController {

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 获取资产负债表
     *
     * @param reportId 报表归档ID
     * @return 资产负债表完整数据
     */
    @GetMapping("/balance-sheet/{reportId}")
    @ApiOperation(value = "获取资产负债表", notes = "获取指定报表的资产负债表完整明细数据")
    public ApiResponse<Object> getBalanceSheet(@PathVariable Long reportId) {
        log.info("获取资产负债表：reportId={}", reportId);
        Object balanceSheet = financialReportService.getBalanceSheetData(reportId);
        return ApiResponse.success(balanceSheet);
    }

    /**
     * 获取利润表
     *
     * @param reportId 报表归档ID
     * @return 利润表完整数据
     */
    @GetMapping("/income-statement/{reportId}")
    @ApiOperation(value = "获取利润表", notes = "获取指定报表的利润表完整明细数据")
    public ApiResponse<Object> getIncomeStatement(@PathVariable Long reportId) {
        log.info("获取利润表：reportId={}", reportId);
        Object incomeStatement = financialReportService.getIncomeStatementData(reportId);
        return ApiResponse.success(incomeStatement);
    }

    /**
     * 获取现金流量表
     *
     * @param reportId 报表归档ID
     * @return 现金流量表完整数据
     */
    @GetMapping("/cash-flow/{reportId}")
    @ApiOperation(value = "获取现金流量表", notes = "获取指定报表的现金流量表完整明细数据")
    public ApiResponse<Object> getCashFlowStatement(@PathVariable Long reportId) {
        log.info("获取现金流量表：reportId={}", reportId);
        Object cashFlowStatement = financialReportService.getCashFlowStatementData(reportId);
        return ApiResponse.success(cashFlowStatement);
    }

    /**
     * 保存资产负债表数据
     *
     * @param reportId 报表ID
     * @param items    明细项列表
     * @return 操作结果
     */
    @PostMapping("/balance-sheet/{reportId}")
    @ApiOperation(value = "保存资产负债表", notes = "保存或更新资产负债表的明细数据")
    public ApiResponse<Void> saveBalanceSheet(@PathVariable Long reportId,
                                               @RequestBody List<com.xinsulu.entity.BalanceSheetItem> items) {
        log.info("保存资产负债表：reportId={}, items={}", reportId, items.size());
        financialReportService.saveBalanceSheet(reportId, items);
        return ApiResponse.success();
    }

    /**
     * 保存利润表数据
     *
     * @param reportId 报表ID
     * @param items    明细项列表
     * @return 操作结果
     */
    @PostMapping("/income-statement/{reportId}")
    @ApiOperation(value = "保存利润表", notes = "保存或更新利润表的明细数据")
    public ApiResponse<Void> saveIncomeStatement(@PathVariable Long reportId,
                                                  @RequestBody List<com.xinsulu.entity.IncomeStatementItem> items) {
        log.info("保存利润表：reportId={}, items={}", reportId, items.size());
        financialReportService.saveIncomeStatement(reportId, items);
        return ApiResponse.success();
    }

    /**
     * 保存现金流量表数据
     *
     * @param reportId 报表ID
     * @param items    明细项列表
     * @return 操作结果
     */
    @PostMapping("/cash-flow/{reportId}")
    @ApiOperation(value = "保存现金流量表", notes = "保存或更新现金流量表的明细数据")
    public ApiResponse<Void> saveCashFlowStatement(@PathVariable Long reportId,
                                                    @RequestBody List<com.xinsulu.entity.CashFlowStatementItem> items) {
        log.info("保存现金流量表：reportId={}, items={}", reportId, items.size());
        financialReportService.saveCashFlowStatement(reportId, items);
        return ApiResponse.success();
    }
}
