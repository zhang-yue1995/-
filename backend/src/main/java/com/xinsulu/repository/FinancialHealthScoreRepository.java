package com.xinsulu.repository;

import com.xinsulu.entity.FinancialHealthScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 财务健康评分数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FinancialHealthScoreRepository extends JpaRepository<FinancialHealthScore, Long> {

    /**
     * 根据报表ID查询健康评分
     *
     * @param reportId 报表ID
     * @return 健康评分对象
     */
    Optional<FinancialHealthScore> findByReportId(Long reportId);

    /**
     * 根据企业ID查询健康评分列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 健康评分分页列表
     */
    Page<FinancialHealthScore> findByEnterpriseIdOrderByReportDateDesc(Long enterpriseId, Pageable pageable);

    /**
     * 根据企业ID和报告日期查询
     *
     * @param enterpriseId 企业ID
     * @param reportDate   报告日期
     * @return 健康评分对象
     */
    Optional<FinancialHealthScore> findByEnterpriseIdAndReportDate(Long enterpriseId, LocalDate reportDate);

    /**
     * 根据风险等级查询
     *
     * @param riskLevel 风险等级
     * @param pageable  分页参数
     * @return 健康评分分页列表
     */
    Page<FinancialHealthScore> findByRiskLevelOrderByReportDateDesc(String riskLevel, Pageable pageable);

    /**
     * 查询最新的健康评分
     *
     * @param enterpriseId 企业ID
     * @return 最新健康评分
     */
    Optional<FinancialHealthScore> findFirstByEnterpriseIdOrderByReportDateDesc(Long enterpriseId);

    /**
     * 根据企业ID和日期范围查询
     *
     * @param enterpriseId 企业ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 健康评分列表
     */
    List<FinancialHealthScore> findByEnterpriseIdAndReportDateBetweenOrderByReportDateAsc(
            Long enterpriseId, LocalDate startDate, LocalDate endDate);

    /**
     * 统计各风险等级的数量
     *
     * @param riskLevel 风险等级
     * @return 数量
     */
    long countByRiskLevel(String riskLevel);

    /**
     * 根据报表归档ID查询健康评分
     *
     * @param archiveId 报表归档ID
     * @return 健康评分对象
     */
    Optional<FinancialHealthScore> findFirstByReportIdAndDeletedOrderByIdDesc(
            Long reportId, Integer deleted);
}
