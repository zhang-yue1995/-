package com.xinsulu.service;

import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.ReportArchiveDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.vo.ReportDetailVO;
import java.util.List;

/**
 * 报表管理服务接口
 *
 * @author xinsulu-team
 */
public interface FinancialReportService {

    /**
     * 创建报表归档记录
     *
     * @param reportArchiveDTO 报表归档信息
     * @return 归档ID
     */
    Long createArchive(ReportArchiveDTO reportArchiveDTO);

    /** 管理端企业资料与报表归档原子入库。 */
    Long createIntake(com.xinsulu.dto.ReportIntakeDTO intakeDTO);

    /**
     * 查询报表详情（包含三大表摘要）
     *
     * @param archiveId 归档ID
     * @return 报表详情
     */
    ReportDetailVO getReportDetail(Long archiveId);

    /**
     * 分页查询企业的报表列表
     *
     * @param enterpriseId 企业ID
     * @param pageQueryDTO 分页参数
     * @return 报表分页列表
     */
    PageResponse<ReportDetailVO> getReportsByEnterprise(Long enterpriseId, PageQueryDTO pageQueryDTO);

    /**
     * 分页查询报表列表，支持企业、报告期和填报状态筛选。
     */
    PageResponse<ReportDetailVO> getReports(Long enterpriseId, String period, String status,
                                            PageQueryDTO pageQueryDTO);

    /**
     * 删除报表归档及关联数据
     *
     * @param archiveId 归档ID
     */
    void deleteArchive(Long archiveId);

    /**
     * 更新报表填报状态
     *
     * @param archiveId 归档ID
     * @param status    状态
     */
    void updateFilingStatus(Long archiveId, String status);

    /**
     * 字段复核与更新
     *
     * @param reportId 报表ID
     * @param reviews  复核结果列表
     */
    void reviewOcrFields(Long reportId, List<com.xinsulu.dto.OcrFieldReviewDTO> reviews);

    /**
     * 保存资产负债表数据
     *
     * @param reportId 报表ID
     * @param items    资产负债表明细项
     */
    void saveBalanceSheet(Long reportId, List<com.xinsulu.entity.BalanceSheetItem> items);

    /**
     * 保存利润表数据
     *
     * @param reportId 报表ID
     * @param items    利润表明细项
     */
    void saveIncomeStatement(Long reportId, List<com.xinsulu.entity.IncomeStatementItem> items);

    /**
     * 保存现金流量表数据
     *
     * @param reportId 报表ID
     * @param items    现金流量表明细项
     */
    void saveCashFlowStatement(Long reportId, List<com.xinsulu.entity.CashFlowStatementItem> items);

    /**
     * 财务指标计算
     *
     * @param reportId 报表ID
     * @return 财务指标Map
     */
    java.util.Map<String, Object> calculateIndicators(Long reportId);

    /**
     * 健康度评分计算
     *
     * @param reportId 报表ID
     * @return 健康度评分VO
     */
    com.xinsulu.vo.HealthScoreVO calculateHealthScore(Long reportId);

    /**
     * 数据校验（勾稽关系校验）
     *
     * @param reportId 报表ID
     * @return 校验结果列表
     */
    java.util.List<java.util.Map<String, Object>> validateFinancialData(Long reportId);

    /**
     * 生成分析报告
     *
     * @param reportId 报表ID
     * @return 分析报告VO
     */
    com.xinsulu.vo.AnalysisReportVO generateAnalysisReport(Long reportId);

    /**
     * 获取已生成的分析报告；尚未生成时自动生成。
     */
    com.xinsulu.vo.AnalysisReportVO getAnalysisReport(Long reportId);

    /**
     * 将分析报告提交审批。
     */
    com.xinsulu.vo.AnalysisReportVO submitAnalysisReport(Long reportId, String username);

    /**
     * 管理员完成分析报告审批。
     */
    com.xinsulu.vo.AnalysisReportVO completeAnalysisApproval(Long reportId, String username);

    /** 管理员驳回整份报表，提交人可修订后重新提交。 */
    com.xinsulu.vo.AnalysisReportVO rejectAnalysisApproval(Long reportId, String username, String reason);

    /**
     * 历史趋势数据查询
     *
     * @param enterpriseId   企业ID
     * @param indicatorCode  指标编码
     * @param periods        期数
     * @return 趋势数据VO
     */
    com.xinsulu.vo.TrendVO getTrendData(Long enterpriseId, String indicatorCode, Integer periods);

    /**
     * 获取最新分析概览
     *
     * @param enterpriseId 企业ID
     * @return 分析概览数据
     */
    Object getLatestAnalysis(Long enterpriseId);

    /**
     * 获取三大报表数据
     *
     * @param reportId 报表ID
     * @return 三大报表数据
     */
    Object getStatements(Long reportId);

    /**
     * 获取资产负债表数据
     *
     * @param reportId 报表ID
     * @return 资产负债表数据
     */
    Object getBalanceSheetData(Long reportId);

    /**
     * 获取利润表数据
     *
     * @param reportId 报表ID
     * @return 利润表数据
     */
    Object getIncomeStatementData(Long reportId);

    /**
     * 获取现金流量表数据
     *
     * @param reportId 报表ID
     * @return 现金流量表数据
     */
    Object getCashFlowStatementData(Long reportId);

    /**
     * 获取健康评分趋势
     *
     * @param enterpriseId 企业ID
     * @return 健康评分趋势列表
     */
    java.util.List<java.util.Map<String, Object>> getHealthScoreTrend(Long enterpriseId);
}
