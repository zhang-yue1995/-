package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.TrendVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 趋势分析控制器
 * 提供历史趋势数据查询、多指标对比、趋势摘要等功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/trends")
@Api(tags = "趋势分析")
public class TrendController {

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 获取企业历史趋势数据
     * 指定指标在多个报告期的变化趋势
     *
     * @param enterpriseId  企业ID
     * @param indicatorCode 指标编码（可选，默认返回主要指标）
     * @param periods       查询期数（默认5期）
     * @return 趋势数据
     */
    @GetMapping("/enterprise/{enterpriseId}")
    @ApiOperation(value = "获取趋势数据", notes = "查询指定财务指标的历史变化趋势，支持同比环比计算")
    public ApiResponse<TrendVO> getTrendData(
            @PathVariable Long enterpriseId,
            @RequestParam(required = false, defaultValue = "roe") String indicatorCode,
            @RequestParam(required = false, defaultValue = "5") Integer periods) {
        log.info("获取趋势数据：enterpriseId={}, indicator={}, periods={}",
                enterpriseId, indicatorCode, periods);

        TrendVO trendData = financialReportService.getTrendData(enterpriseId, indicatorCode, periods);
        return ApiResponse.success(trendData);
    }

    /**
     * 多指标对比趋势
     * 同时查看多个关键指标的走势对比
     *
     * @param enterpriseId 企业ID
     * @param periods      期数
     * @return 多指标趋势数据
     */
    @GetMapping("/enterprise/{enterpriseId}/indicators")
    @ApiOperation(value = "多指标对比", notes = "同时查看多个核心财务指标的历史变化趋势")
    public ApiResponse<List<TrendVO>> getMultiIndicatorTrends(
            @PathVariable Long enterpriseId,
            @RequestParam(required = false, defaultValue = "5") Integer periods) {
        log.info("多指标对比趋势：enterpriseId={}, periods={}", enterpriseId, periods);

        // 定义核心指标列表
        String[] coreIndicators = {
                "debtToAssetRatio",   // 资产负债率
                "roe",                 // 净资产收益率
                "currentRatio",        // 流动比率
                "netProfitMargin",     // 销售净利率
                "operatingCashToNetProfit" // 经营现金/净利润
        };

        List<TrendVO> trends = new ArrayList<>();
        for (String indicator : coreIndicators) {
            try {
                TrendVO trend = financialReportService.getTrendData(enterpriseId, indicator, periods);
                if (trend != null && trend.getDataList() != null && !trend.getDataList().isEmpty()) {
                    trends.add(trend);
                }
            } catch (Exception e) {
                log.warn("获取指标{}的趋势数据失败：{}", indicator, e.getMessage());
            }
        }

        return ApiResponse.success(trends);
    }

    /**
     * 趋势摘要
     * 返回企业财务状况的整体趋势概览
     *
     * @param enterpriseId 企业ID
     * @return 趋势摘要信息
     */
    @GetMapping("/enterprise/{enterpriseId}/summary")
    @ApiOperation(value = "趋势摘要", notes = "生成企业财务状况的整体趋势概览和关键发现")
    public ApiResponse<Map<String, Object>> getTrendSummary(@PathVariable Long enterpriseId) {
        log.info("获取趋势摘要：enterpriseId={}", enterpriseId);

        Map<String, Object> summary = new HashMap<>();

        // 获取最近几期的健康评分趋势
        List<Map<String, Object>> healthScoreTrend = financialReportService.getHealthScoreTrend(enterpriseId);
        summary.put("healthScoreTrend", healthScoreTrend);

        // 判断整体趋势方向
        String overallTrend = determineOverallTrend(healthScoreTrend);
        summary.put("overallTrend", overallTrend);
        summary.put("trendDescription", getTrendDescription(overallTrend));

        // 关键发现
        List<String> keyFindings = generateKeyFindingsFromTrend(enterpriseId);
        summary.put("keyFindings", keyFindings);

        log.info("趋势摘要生成完成");
        return ApiResponse.success(summary);
    }

    /**
     * 判断整体趋势方向
     */
    private String determineOverallTrend(List<Map<String, Object>> healthScoreTrend) {
        if (healthScoreTrend == null || healthScoreTrend.size() < 2) {
            return "INSUFFICIENT_DATA";
        }

        // 取最近两期的评分进行比较
        int size = healthScoreTrend.size();
        Object latestObj = healthScoreTrend.get(size - 1).get("score");
        Object previousObj = healthScoreTrend.get(size - 2).get("score");

        if (latestObj instanceof Number && previousObj instanceof Number) {
            double latest = ((Number) latestObj).doubleValue();
            double previous = ((Number) previousObj).doubleValue();

            double change = latest - previous;
            if (change > 3) return "IMPROVING";       // 改善
            else if (change < -3) return "DECLINING"; // 下滑
            else return "STABLE";                      // 稳定
        }

        return "UNKNOWN";
    }

    /**
     * 获取趋势描述
     */
    private String getTrendDescription(String trend) {
        switch (trend) {
            case "IMPROVING": return "整体财务状况呈改善趋势";
            case "DECLINING": return "整体财务状况呈下滑趋势，需关注";
            case "STABLE": return "整体财务状况保持稳定";
            case "INSUFFICIENT_DATA": return "数据不足，无法判断趋势";
            default: return "未知状态";
        }
    }

    /**
     * 从趋势数据中生成关键发现
     */
    private List<String> generateKeyFindingsFromTrend(Long enterpriseId) {
        List<String> findings = new ArrayList<>();

        try {
            // 检查ROE趋势
            TrendVO roeTrend = financialReportService.getTrendData(enterpriseId, "roe", 3);
            if (roeTrend != null && roeTrend.getDataList() != null && roeTrend.getDataList().size() >= 2) {
                int size = roeTrend.getDataList().size();
                TrendVO.TrendDataItem latest = roeTrend.getDataList().get(size - 1);
                TrendVO.TrendDataItem previous = roeTrend.getDataList().get(size - 2);

                if (latest.getValue() != null && previous.getValue() != null) {
                    int cmp = latest.getValue().compareTo(previous.getValue());
                    if (cmp > 0) {
                        findings.add("净资产收益率（ROE）近期呈上升趋势，盈利能力增强");
                    } else if (cmp < 0) {
                        findings.add("净资产收益率（ROE）近期有所下降，需关注盈利能力变化");
                    } else {
                        findings.add("净资产收益率（ROE）保持稳定");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("生成ROE趋势发现失败", e);
        }

        try {
            // 检查资产负债率趋势
            TrendVO debtTrend = financialReportService.getTrendData(enterpriseId, "debtToAssetRatio", 3);
            if (debtTrend != null && debtTrend.getDataList() != null && debtTrend.getDataList().size() >= 2) {
                int size = debtTrend.getDataList().size();
                TrendVO.TrendDataItem latest = debtTrend.getDataList().get(size - 1);
                TrendVO.TrendDataItem previous = debtTrend.getDataList().get(size - 2);

                if (latest.getValue() != null && previous.getValue() != null) {
                    int cmp = latest.getValue().compareTo(previous.getValue());
                    if (cmp > 0) {
                        findings.add("资产负债率近期有所上升，偿债压力增加");
                    } else if (cmp < 0) {
                        findings.add("资产负债率近期下降，财务结构趋于稳健");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("生成资产负债率趋势发现失败", e);
        }

        if (findings.isEmpty()) {
            findings.add("暂无明显趋势变化，建议持续关注各项指标动态");
        }

        return findings;
    }
}
