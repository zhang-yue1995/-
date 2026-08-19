package com.xinsulu.repository;

import com.xinsulu.entity.FinancialAnalysisReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 财务分析报告数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FinancialAnalysisReportRepository extends JpaRepository<FinancialAnalysisReport, Long> {

    /**
     * 根据报表ID查询分析报告
     *
     * @param reportId 报表ID
     * @return 分析报告列表
     */
    List<FinancialAnalysisReport> findByReportIdOrderByCreatedTimeDesc(Long reportId);

    /**
     * 根据企业ID查询分析报告列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 分析报告分页列表
     */
    Page<FinancialAnalysisReport> findByEnterpriseIdOrderByCreatedTimeDesc(Long enterpriseId, Pageable pageable);

    /**
     * 根据企业ID和报告类型查询
     *
     * @param enterpriseId 企业ID
     * @param reportType   报告类型
     * @param pageable     分页参数
     * @return 分析报告分页列表
     */
    Page<FinancialAnalysisReport> findByEnterpriseIdAndReportTypeOrderByCreatedTimeDesc(
            Long enterpriseId, String reportType, Pageable pageable);

    /**
     * 根据状态查询
     *
     * @param status   状态
     * @param pageable 分页参数
     * @return 分析报告分页列表
     */
    Page<FinancialAnalysisReport> findByStatusOrderByCreatedTimeDesc(String status, Pageable pageable);

    /**
     * 查询最新的分析报告
     *
     * @param reportId 报表ID
     * @return 最新分析报告
     */
    Optional<FinancialAnalysisReport> findFirstByReportIdOrderByVersionDesc(Long reportId);

    Optional<FinancialAnalysisReport> findFirstByReportIdAndDeletedOrderByVersionDesc(
            Long reportId, Integer deleted);

    /**
     * 根据生成方法查询
     *
     * @param generationMethod 生成方法
     * @param pageable         分页参数
     * @return 分析报告分页列表
     */
    Page<FinancialAnalysisReport> findByGenerationMethodOrderByCreatedTimeDesc(
            String generationMethod, Pageable pageable);
}
