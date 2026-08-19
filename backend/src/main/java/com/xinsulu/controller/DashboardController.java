package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.entity.Enterprise;
import com.xinsulu.repository.EnterpriseRepository;
import com.xinsulu.repository.FinancialReportArchiveRepository;
import com.xinsulu.repository.FinancialHealthScoreRepository;
import com.xinsulu.repository.OcrFieldResultRepository;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.vo.DashboardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 工作台仪表盘控制器
 * 提供统计数据、最近记录、风险分布、趋势图表等Dashboard数据
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@Api(tags = "工作台仪表盘")
public class DashboardController {

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private FinancialReportArchiveRepository archiveRepository;

    @Autowired
    private FinancialHealthScoreRepository healthScoreRepository;

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    /**
     * 工作台统计数据
     * 包括企业数、报表数、待复核数、高风险企业数等核心指标
     *
     * @return 统计数据
     */
    @GetMapping("/stats")
    @ApiOperation(value = "工作台统计", notes = "获取工作台的核心统计指标：企业总数、报表数量、待处理任务等")
    public ApiResponse<DashboardVO> getStats() {
        log.info("获取工作台统计数据");

        DashboardVO stats = new DashboardVO();

        // 企业总数
        long totalEnterprises = archiveRepository.countDistinctActiveEnterprises();
        stats.setTotalEnterprises(totalEnterprises);

        // 报表总数
        long totalReports = archiveRepository.countActive();
        stats.setTotalReports(totalReports);

        // 本月新增报表
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        long monthlyNewReports = archiveRepository.countActiveCreatedAfter(monthStart);
        stats.setMonthlyNewReports(monthlyNewReports);

        // 待复核报表数：草稿不属于已提交复核任务，避免前端出现无效待办。
        long pendingReview = archiveRepository.countActiveByFilingStatusIn(
                Collections.singletonList("PENDING_REVIEW"));
        stats.setPendingReview(pendingReview);

        // 健康度与风险均以企业最新有效报表的真实评分为准；未评分企业不参与平均值。
        List<Enterprise> activeEnterprises = archiveRepository.findDistinctActiveEnterprises();
        List<com.xinsulu.entity.FinancialHealthScore> latestScores = new ArrayList<>();
        for (Enterprise enterprise : activeEnterprises) {
            List<FinancialReportArchive> latest = archiveRepository
                    .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(
                            enterprise.getId(), 0, PageRequest.of(0, 1));
            if (!latest.isEmpty()) {
                healthScoreRepository.findFirstByReportIdAndDeletedOrderByIdDesc(latest.get(0).getId(), 0)
                        .ifPresent(latestScores::add);
            }
        }
        long highRiskEnterprises = latestScores.stream()
                .filter(score -> "DANGEROUS".equalsIgnoreCase(score.getRiskLevel())
                        || "CRITICAL".equalsIgnoreCase(score.getRiskLevel()))
                .count();
        stats.setHighRiskEnterprises(highRiskEnterprises);
        double avgHealthScore = latestScores.stream()
                .map(com.xinsulu.entity.FinancialHealthScore::getTotalScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).average().orElse(0D);
        stats.setAverageHealthScore(BigDecimal.valueOf(avgHealthScore)
                .setScale(2, java.math.RoundingMode.HALF_UP));

        // OCR识别完成率
        long archivedOcrTasks = ocrTaskRepository.countArchivedActiveTasks();
        long ocrCompleted = ocrTaskRepository.countArchivedActiveTasksByStatus("COMPLETED");
        stats.setOcrCompletionRate(archivedOcrTasks > 0 ?
                new BigDecimal(ocrCompleted * 100.0 / archivedOcrTasks).setScale(2, java.math.RoundingMode.HALF_UP) :
                BigDecimal.ZERO);

        log.info("工作台统计数据获取完成");
        return ApiResponse.success(stats);
    }

    /**
     * 最近上传记录
     * 显示最近上传或创建的报表列表
     *
     * @param limit 返回条数限制
     * @return 最近记录列表
     */
    @GetMapping("/recent-uploads")
    @ApiOperation(value = "最近上传记录", notes = "获取最近上传或创建的报表归档记录")
    public ApiResponse<List<Map<String, Object>>> getRecentUploads(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取最近上传记录：limit={}", limit);

        List<FinancialReportArchive> recentArchives = archiveRepository
                .findRecentActive(PageRequest.of(0, Math.max(1, Math.min(limit, 50))));

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        for (FinancialReportArchive archive : recentArchives) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("archiveId", archive.getId());
            item.put("enterpriseId", archive.getEnterprise().getId());
            item.put("enterpriseName", archive.getEnterprise().getEnterpriseName());
            item.put("reportPeriod", archive.getReportPeriod());
            item.put("reportType", archive.getReportType());
            item.put("filingStatus", archive.getFilingStatus());
            item.put("dataSource", archive.getDataSource());
            item.put("createdTime", archive.getCreatedTime().format(formatter));
            item.put("dataQualityScore", archive.getDataQualityScore());

            result.add(item);
        }

        return ApiResponse.success(result);
    }

    /**
     * 风险等级分布
     * 统计各风险等级的企业数量分布
     *
     * @return 风险分布数据
     */
    @GetMapping("/risk-distribution")
    @ApiOperation(value = "风险等级分布", notes = "统计各风险等级的企业数量分布情况")
    public ApiResponse<List<Map<String, Object>>> getRiskDistribution() {
        log.info("获取风险等级分布");

        List<Map<String, Object>> distribution = new ArrayList<>();

        // 统计各风险等级的数量
        String[] riskLevels = {"HEALTHY", "NORMAL", "ATTENTION", "DANGEROUS", "CRITICAL"};
        String[] riskLabels = {"健康", "基本健康", "需关注", "高风险", "严重风险"};
        String[] colors = {"#52c41a", "#1890ff", "#faad14", "#ff4d4f", "#cf1322"};

        List<Enterprise> activeEnterprises = archiveRepository.findDistinctActiveEnterprises();
        Map<String, Long> actualCounts = new HashMap<>();
        for (Enterprise enterprise : activeEnterprises) {
            List<FinancialReportArchive> latest = archiveRepository
                    .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(
                            enterprise.getId(), 0, PageRequest.of(0, 1));
            if (latest.isEmpty()) continue;
            healthScoreRepository.findFirstByReportIdAndDeletedOrderByIdDesc(latest.get(0).getId(), 0)
                    .map(com.xinsulu.entity.FinancialHealthScore::getRiskLevel)
                    .filter(Objects::nonNull)
                    .ifPresent(level -> actualCounts.merge(level, 1L, Long::sum));
        }
        for (int i = 0; i < riskLevels.length; i++) {
            long count = actualCounts.getOrDefault(riskLevels[i], 0L);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("level", riskLevels[i]);
            item.put("label", riskLabels[i]);
            item.put("count", count);
            item.put("color", colors[i]);

            distribution.add(item);
        }

        return ApiResponse.success(distribution);
    }

    /**
     * 关键指标趋势图数据
     * 提供用于绘制趋势图的数据点
     *
     * @param days 统计天数（默认30天）
     * @return 趋势图数据
     */
    @GetMapping("/trend-chart")
    @ApiOperation(value = "关键指标趋势", notes = "获取近期的关键财务指标趋势数据，用于绘制图表")
    public ApiResponse<Map<String, Object>> getTrendChartData(
            @RequestParam(defaultValue = "30") Integer days) {
        log.info("获取趋势图数据：days={}", days);

        Map<String, Object> chartData = new LinkedHashMap<>();

        // 时间轴标签（日期列表）
        List<String> labels = new ArrayList<>();
        List<Integer> reportCounts = new ArrayList<>();
        List<Double> avgScores = new ArrayList<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);

        // 按天统计（简化实现：实际可按周/月聚合）
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            labels.add(current.format(DateTimeFormatter.ofPattern("MM-dd")));

            // 当天新增报表数
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = current.plusDays(1).atStartOfDay();
            long dayCount = archiveRepository.countActiveCreatedBetween(dayStart, dayEnd);
            reportCounts.add((int) dayCount);

            // 当天入库报表所属企业的平均健康评分
            avgScores.add(archiveRepository.findAverageHealthScoreByArchiveDate(dayStart, dayEnd));

            current = current.plusDays(1);
        }

        chartData.put("labels", labels);
        chartData.put("reportCounts", reportCounts);
        chartData.put("avgScores", avgScores);

        // 仅基于真实入库记录计算高峰日期。
        Map<String, Object> peakAnalysis = new LinkedHashMap<>();
        int peakIndex = 0;
        for (int i = 1; i < reportCounts.size(); i++) {
            if (reportCounts.get(i) > reportCounts.get(peakIndex)) {
                peakIndex = i;
            }
        }
        peakAnalysis.put("peakDay", reportCounts.isEmpty() || reportCounts.get(peakIndex) == 0
                ? null : labels.get(peakIndex));
        peakAnalysis.put("peakHour", null);
        peakAnalysis.put("avgDailyReports", reportCounts.stream()
                .mapToInt(Integer::intValue).average().orElse(0));
        chartData.put("peakAnalysis", peakAnalysis);

        log.info("趋势图数据获取完成");
        return ApiResponse.success(chartData);
    }

    /**
     * 待办事项提醒
     * 返回需要用户关注的待办事项列表
     *
     * @return 待办事项列表
     */
    @GetMapping("/todo-items")
    @ApiOperation(value = "待办事项", notes = "获取需要处理的待办事项提醒")
    public ApiResponse<List<Map<String, Object>>> getTodoItems() {
        log.info("获取待办事项");

        List<Map<String, Object>> todoItems = new ArrayList<>();

        // 待复核报表
        long pendingReview = archiveRepository.countActiveByFilingStatusIn(
                Collections.singletonList("PENDING_REVIEW"));
        if (pendingReview > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "REVIEW");
            item.put("title", "待复核报表");
            item.put("count", pendingReview);
            item.put("priority", "HIGH");
            item.put("description", "有" + pendingReview + "份报表等待人工复核");
            todoItems.add(item);
        }

        long lowConfidenceFields = ocrFieldResultRepository
                .countByConfidenceLevelAndIsReviewedAndDeleted("LOW", 0, 0);
        if (lowConfidenceFields > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "LOW_CONFIDENCE");
            item.put("title", "低置信度字段");
            item.put("count", lowConfidenceFields);
            item.put("priority", "MEDIUM");
            item.put("description", "有" + lowConfidenceFields + "个识别字段需要人工确认");
            todoItems.add(item);
        }

        return ApiResponse.success(todoItems);
    }
}
