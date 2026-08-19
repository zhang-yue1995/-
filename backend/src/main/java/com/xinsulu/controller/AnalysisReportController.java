package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.config.ApiAuthInterceptor;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.service.pdf.FinancialAnalysisPdfGenerator;
import com.xinsulu.vo.AnalysisReportVO;
import com.xinsulu.vo.HealthScoreVO;
import com.xinsulu.vo.ReportDetailVO;
import com.xinsulu.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 分析报告控制器
 * 提供财务分析报告的生成、查询、导出功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/analysis-reports")
@Api(tags = "分析报告管理")
public class AnalysisReportController {

    @Autowired
    private FinancialReportService financialReportService;

    @Autowired
    private FinancialAnalysisPdfGenerator pdfGenerator;

    /**
     * 获取分析报告
     * 基于真实计算结果生成的完整财务分析报告
     *
     * @param reportId 报表ID
     * @return 分析报告
     */
    @GetMapping("/report/{reportId}")
    @ApiOperation(value = "获取分析报告", notes = "获取基于规则引擎自动生成的完整财务分析报告")
    public ApiResponse<AnalysisReportVO> getAnalysisReport(@PathVariable Long reportId) {
        log.info("获取分析报告：reportId={}", reportId);
        AnalysisReportVO report = financialReportService.getAnalysisReport(reportId);
        return ApiResponse.success(report);
    }

    /**
     * 将智能分析报告提交后台审批。
     */
    @PostMapping("/{reportId}/submit-approval")
    @ApiOperation(value = "提交报告审批", notes = "将已生成的智能分析报告提交至管理后台审批")
    public ApiResponse<AnalysisReportVO> submitApproval(
            @PathVariable Long reportId,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        log.info("提交分析报告审批：reportId={}, username={}", reportId, currentUser.getUsername());
        return ApiResponse.success(financialReportService.submitAnalysisReport(
                reportId, currentUser.getUsername()));
    }

    /**
     * 重新生成分析报告
     * 强制重新计算所有指标并生成新的分析报告
     *
     * @param reportId 报表ID
     * @return 新的分析报告
     */
    @PostMapping("/generate/{reportId}")
    @ApiOperation(value = "重新生成报告", notes = "强制重新计算指标并生成最新的分析报告")
    public ApiResponse<AnalysisReportVO> regenerateReport(@PathVariable Long reportId) {
        log.info("重新生成分析报告：reportId={}", reportId);
        AnalysisReportVO report = financialReportService.generateAnalysisReport(reportId);
        return ApiResponse.success(report);
    }

    /**
     * 导出报告
     * 以 PDF 格式导出分析报告内容
     *
     * @param reportId 报表ID
     * @return PDF 格式的报告内容
     */
    @GetMapping("/export/{reportId}")
    @ApiOperation(value = "导出报告", notes = "导出排版完整的PDF智能分析报告")
    public ResponseEntity<byte[]> exportReport(@PathVariable Long reportId) {
        log.info("导出分析报告：reportId={}", reportId);

        AnalysisReportVO report = financialReportService.getAnalysisReport(reportId);
        ReportDetailVO detail = financialReportService.getReportDetail(reportId);
        Map<String, Object> indicators = financialReportService.calculateIndicators(reportId);
        HealthScoreVO healthScore = financialReportService.calculateHealthScore(reportId);
        byte[] pdf = pdfGenerator.generate(report, detail, healthScore, indicators);

        String enterpriseName = safeFilename(report.getEnterpriseName(), "企业");
        String reportPeriod = safeFilename(report.getReportPeriod(), "未标注期间");
        String filename = enterpriseName + "_" + reportPeriod + "_报表分析.pdf";
        String encodedFilename = org.springframework.web.util.UriUtils.encode(
                filename, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encodedFilename)
                .body(pdf);
    }

    private String safeFilename(String value, String fallback) {
        String normalized = value == null || value.trim().isEmpty() ? fallback : value.trim();
        return normalized.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
