package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.HealthScoreVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 财务指标控制器
 * 提供财务指标计算、健康度评分、指标定义查询等接口
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/indicators")
@Api(tags = "财务指标管理")
public class IndicatorController {

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 获取报表的所有财务指标
     * 包含30+个核心财务指标的实时计算结果
     *
     * @param reportId 报表ID
     * @return 财务指标Map
     */
    @GetMapping("/report/{reportId}")
    @ApiOperation(value = "获取财务指标", notes = "计算并返回30+个核心财务指标，包括偿债、盈利、运营、现金流、成长能力")
    public ApiResponse<Map<String, Object>> getIndicators(@PathVariable Long reportId) {
        log.info("计算财务指标：reportId={}", reportId);
        Map<String, Object> indicators = financialReportService.calculateIndicators(reportId);
        return ApiResponse.success(indicators);
    }

    /**
     * 获取健康度评分
     * 五维评分模型：偿债30% + 盈利25% + 现金流20% + 运营15% + 成长10%
     *
     * @param reportId 报表ID
     * @return 健康度评分详情
     */
    @GetMapping("/report/{reportId}/health-score")
    @ApiOperation(value = "健康度评分", notes = "基于五维模型计算企业财务健康评分，返回各维度得分和风险等级")
    public ApiResponse<HealthScoreVO> getHealthScore(@PathVariable Long reportId) {
        log.info("计算健康度评分：reportId={}", reportId);
        HealthScoreVO healthScore = financialReportService.calculateHealthScore(reportId);
        return ApiResponse.success(healthScore);
    }

    /**
     * 获取指标定义和公式说明
     * 返回所有支持的财务指标及其计算公式和说明
     *
     * @return 指标定义列表
     */
    @GetMapping("/definitions")
    @ApiOperation(value = "指标定义", notes = "获取所有财务指标的定义、计算公式和参考标准")
    public ApiResponse<Map<String, Object>> getIndicatorDefinitions() {
        log.info("获取指标定义");

        Map<String, Object> definitions = new LinkedHashMap<>();

        // 短期偿债能力指标
        Map<String, Object> solvencyShort = new LinkedHashMap<>();
        solvencyShort.put("currentRatio", createIndicatorDef("流动比率",
                "流动资产 / 流动负债", "衡量短期偿债能力的核心指标，一般>=2为佳"));
        solvencyShort.put("quickRatio", createIndicatorDef("速动比率",
                "(流动资产 - 存货) / 流动负债", "剔除存货后的短期偿债能力，一般>=1为佳"));
        solvencyShort.put("conservativeQuickRatio", createIndicatorDef("保守速动比率",
                "(货币资金 + 应收票据 + 应收账款) / 流动负债", "最保守的短期偿债能力指标"));
        solvencyShort.put("cashRatio", createIndicatorDef("现金比率",
                "货币资金 / 流动负债", "即时偿债能力，一般>=0.2为宜"));
        solvencyShort.put("workingCapital", createIndicatorDef("营运资本",
                "流动资产 - 流动负债", "运营资金的充裕程度，正值表示流动性充足"));
        definitions.put("短期偿债能力", solvencyShort);

        // 长期偿债能力指标
        Map<String, Object> solvencyLong = new LinkedHashMap<>();
        solvencyLong.put("debtToAssetRatio", createIndicatorDef("资产负债率",
                "总负债 / 总资产 × 100%", "反映总资产中有多大比例通过借债筹集，40-60%为宜"));
        solvencyLong.put("debtToEquityRatio", createIndicatorDef("产权比率",
                "总负债 / 所有者权益", "债权人提供的资本与股东资本的比率关系"));
        solvencyLong.put("tangibleDebtRatio", createIndicatorDef("有形净值债务率",
                "总负债 / (所有者权益 - 无形资产)", "更保守的长期偿债能力指标"));
        solvencyLong.put("interestCoverageRatio", createIndicatorDef("利息保障倍数",
                "(利润总额 + 利息支出) / 利息支出", "支付利息的能力，一般>=3为宜"));
        solvencyLong.put("longTermDebtToWorkingCapital", createIndicatorDef("长期债务与营运资金比率",
                "非流动负债 / (流动资产 - 流动负债)", "长期债务与短期偿债储备的关系"));
        definitions.put("长期偿债能力", solvencyLong);

        // 盈利能力指标
        Map<String, Object> profitability = new LinkedHashMap<>();
        profitability.put("grossProfitMargin", createIndicatorDef("销售毛利率",
                "(营业收入 - 营业成本) / 营业收入 × 100%", "产品的基本盈利能力，越高越好"));
        profitability.put("operatingProfitMargin", createIndicatorDef("营业利润率",
                "营业利润 / 营业收入 × 100%", "经营活动的盈利效率"));
        profitability.put("netProfitMargin", createIndicatorDef("销售净利率",
                "净利润 / 营业收入 × 100%", "最终获利能力，一般>=10%为良好"));
        profitability.put("costExpenseProfitMargin", createIndicatorDef("成本费用利润率",
                "利润总额 / 成本费用总额 × 100%", "成本费用的产出效率"));
        profitability.put("roa", createIndicatorDef("总资产收益率ROA",
                "净利润 / 平均总资产 × 100%", "全部资产的盈利能力，一般>=5%为良好"));
        profitability.put("roe", createIndicatorDef("净资产收益率ROE",
                "净利润 / 平均所有者权益 × 100%", "股东权益的回报率，一般>=15%为优秀"));
        definitions.put("盈利能力", profitability);

        // 运营效率指标
        Map<String, Object> operation = new LinkedHashMap<>();
        operation.put("accountsReceivableTurnover", createIndicatorDef("应收账款周转率",
                "营业收入 / 平均应收账款", "收款速度，越高越好，一般>=6次/年"));
        operation.put("accountsReceivableTurnoverDays", createIndicatorDef("应收账款周转天数",
                "365 / 应收账款周转率", "平均收账期，越短越好，一般<=60天"));
        operation.put("inventoryTurnover", createIndicatorDef("存货周转率",
                "营业成本 / 平均存货", "存货变现速度，越高越好，一般>=4次/年"));
        operation.put("inventoryTurnoverDays", createIndicatorDef("存货周转天数",
                "365 / 存货周转率", "存货占用周期，越短越好，一般<=90天"));
        operation.put("currentAssetTurnover", createIndicatorDef("流动资产周转率",
                "营业收入 / 平均流动资产", "流动资产的利用效率"));
        operation.put("fixedAssetTurnover", createIndicatorDef("固定资产周转率",
                "营业收入 / 固定资产净值", "固定资产的利用效率"));
        operation.put("totalAssetTurnover", createIndicatorDef("总资产周转率",
                "营业收入 / 平均总资产", "全部资产的运营效率，一般>=0.8次/年"));
        definitions.put("运营效率", operation);

        // 现金流能力指标
        Map<String, Object> cashFlow = new LinkedHashMap<>();
        cashFlow.put("cashFlowRatio", createIndicatorDef("现金流量比率",
                "经营活动现金流净额 / 流动负债", "用经营现金流偿还短期负债的能力"));
        cashFlow.put("cashDebtRatio", createIndicatorDef("现金负债比率",
                "经营活动现金流净额 / 总负债", "用经营现金流偿还全部负债的能力"));
        cashFlow.put("operatingCashToNetProfit", createIndicatorDef("经营现金净流量与净利润比率",
                "经营现金流净额 / 净利润", "盈利的质量，>=1表示盈利有现金流支撑"));
        cashFlow.put("operatingCashToRevenue", createIndicatorDef("经营现金净流量与营业收入比率",
                "经营现金流净额 / 营业收入", "销售收入的现金回收质量"));
        cashFlow.put("cashFlowInterestCoverage", createIndicatorDef("现金流量利息保障倍数",
                "经营现金流净额 / 利息支出", "用实际现金流支付利息的能力"));
        definitions.put("现金流能力", cashFlow);

        // 成长能力指标
        Map<String, Object> growth = new LinkedHashMap<>();
        growth.put("revenueGrowthRate", createIndicatorDef("营业收入增长率",
                "(本期营收 - 上期营收) / 上期营收 × 100%", "业务扩张速度，一般>=10%为良好"));
        growth.put("netProfitGrowthRate", createIndicatorDef("净利润增长率",
                "(本期净利润 - 上期净利润) / 上期净利润 × 100%", "盈利增长速度"));
        growth.put("totalAssetGrowthRate", createIndicatorDef("总资产增长率",
                "(本期总资产 - 上期总资产) / 上期总资产 × 100%", "规模扩张速度"));
        growth.put("equityGrowthRate", createIndicatorDef("所有者权益增长率",
                "(本期权益 - 上期权益) / 上期权益 × 100%", "股东财富积累速度"));
        growth.put("operatingCashFlowGrowthRate", createIndicatorDef("经营现金流增长率",
                "(本期经营现金流 - 上期经营现金流) / 上期经营现金流 × 100%", "造血能力变化趋势"));
        definitions.put("成长能力", growth);

        return ApiResponse.success(definitions);
    }

    /**
     * 创建指标定义对象
     */
    private Map<String, String> createIndicatorDef(String name, String formula, String description) {
        Map<String, String> def = new LinkedHashMap<>();
        def.put("name", name);
        def.put("formula", formula);
        def.put("description", description);
        return def;
    }
}
