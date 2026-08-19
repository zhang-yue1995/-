package com.xinsulu.repository;

import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.entity.Enterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 财务报表归档数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FinancialReportArchiveRepository extends JpaRepository<FinancialReportArchive, Long>,
        JpaSpecificationExecutor<FinancialReportArchive> {

    /**
     * 根据企业和报告期查询报表
     *
     * @param enterpriseId 企业ID
     * @param reportPeriod 报告期
     * @return 报表归档对象
     */
    Optional<FinancialReportArchive> findByEnterpriseIdAndReportPeriod(Long enterpriseId, String reportPeriod);

    Optional<FinancialReportArchive> findFirstByEnterpriseIdAndReportPeriodAndDeleted(
            Long enterpriseId, String reportPeriod, Integer deleted);

    List<FinancialReportArchive> findAllByEnterpriseIdAndDeleted(Long enterpriseId, Integer deleted);

    /**
     * 根据企业ID查询报表列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 报表分页列表
     */
    /**
     * 统计企业的报表数量
     *
     * @param enterpriseId 企业ID
     * @return 报表数量
     */
    long countByEnterpriseId(Long enterpriseId);

    /**
     * 统计企业未删除的报表数量
     *
     * @param enterpriseId 企业ID
     * @param deleted      删除标记
     * @return 报表数量
     */
    long countByEnterpriseIdAndDeleted(Long enterpriseId, Integer deleted);

    /**
     * 查询最新的报表
     *
     * @param enterpriseId 企业ID
     * @return 最新报表
     */
    Optional<FinancialReportArchive> findFirstByEnterpriseIdOrderByCreatedTimeDesc(Long enterpriseId);

    /**
     * 查询指定日期之前的报表（用于历史趋势）
     *
     * @param enterpriseId 企业ID
     * @param beforeTime  时间点
     * @param deleted     删除标记
     * @return 报表列表（按日期降序）
     */
    List<FinancialReportArchive> findByEnterpriseIdAndCreatedTimeBeforeAndDeletedOrderByCreatedTimeDesc(
            Long enterpriseId, LocalDateTime beforeTime, Integer deleted);

    /**
     * 查询最近N期报表（用于趋势分析）
     *
     * @param enterpriseId 企业ID
     * @param deleted      删除标记
     * @param limit        限制条数
     * @return 报表列表
     */
    @Query("SELECT a FROM FinancialReportArchive a WHERE a.enterprise.id = :enterpriseId AND a.deleted = :deleted " +
            "ORDER BY a.reportYear DESC, a.reportMonth DESC, a.reportQuarter DESC, a.createdTime DESC")
    List<FinancialReportArchive> findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(
            @Param("enterpriseId") Long enterpriseId,
            @Param("deleted") Integer deleted,
            Pageable pageable);

    /**
     * 根据填报状态列表统计数量
     *
     * @param filingStatusList 填报状态列表
     * @param deleted          删除标记
     * @return 数量
     */
    long countByFilingStatusInAndDeleted(List<String> filingStatusList, Integer deleted);

    /**
     * 根据企业和删除标记分页查询（支持Pageable参数）
     *
     * @param enterpriseId 企业ID
     * @param deleted      删除标记
     * @param pageable     分页参数
     * @return 报表分页列表
     */
    Page<FinancialReportArchive> findAllByEnterpriseIdAndDeleted(Long enterpriseId, Integer deleted, Pageable pageable);

    /**
     * 统计未删除的报表总数
     *
     * @param deleted 删除标记
     * @return 报表数量
     */
    long countByDeleted(Integer deleted);

    @Query("SELECT COUNT(a) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0")
    long countActive();

    @Query("SELECT COUNT(DISTINCT e.id) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0")
    long countDistinctActiveEnterprises();

    @Query("SELECT COUNT(DISTINCT e.id) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 AND e.riskLevel IN :riskLevels")
    long countDistinctActiveEnterprisesByRiskLevelIn(@Param("riskLevels") List<String> riskLevels);

    @Query("SELECT COUNT(DISTINCT e.id) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 AND e.riskLevel = :riskLevel")
    long countDistinctActiveEnterprisesByRiskLevel(@Param("riskLevel") String riskLevel);

    @Query("SELECT DISTINCT e FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0")
    List<Enterprise> findDistinctActiveEnterprises();

    /**
     * 统计指定时间之后的报表数（本月新增）
     *
     * @param afterTime 时间点
     * @param deleted   删除标记
     * @return 报表数量
     */
    long countByCreatedTimeAfterAndDeleted(LocalDateTime afterTime, Integer deleted);

    /**
     * 查询最近N条报表记录
     *
     * @param deleted 删除标记
     * @param limit   限制条数
     * @return 报表列表
     */
    List<FinancialReportArchive> findTop10ByDeletedOrderByCreatedTimeDesc(Integer deleted);

    @Query("SELECT a FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 ORDER BY a.createdTime DESC")
    List<FinancialReportArchive> findRecentActive(Pageable pageable);

    @Query("SELECT COUNT(a) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 AND a.filingStatus IN :statuses")
    long countActiveByFilingStatusIn(@Param("statuses") List<String> statuses);

    @Query("SELECT COUNT(a) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 AND a.createdTime >= :afterTime")
    long countActiveCreatedAfter(@Param("afterTime") LocalDateTime afterTime);

    /**
     * 统计日期范围内的报表数
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param deleted   删除标记
     * @return 报表数量
     */
    long countByCreatedTimeBetweenAndDeleted(LocalDateTime startTime, LocalDateTime endTime, Integer deleted);

    @Query("SELECT COUNT(a) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.deleted = 0 AND e.deleted = 0 AND a.createdTime >= :startTime AND a.createdTime < :endTime")
    long countActiveCreatedBetween(@Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 计算指定入库日期范围内报表所属企业的平均健康评分。
     *
     * @return 平均健康评分
     */
    @Query("SELECT AVG(e.healthScore) FROM FinancialReportArchive a JOIN a.enterprise e " +
            "WHERE a.createdTime >= :startTime AND a.createdTime < :endTime " +
            "AND a.deleted = 0 AND e.deleted = 0 AND e.healthScore IS NOT NULL")
    Double findAverageHealthScoreByArchiveDate(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 根据数据源统计报表数
     */
    long countByDataSourceAndDeleted(String dataSource, Integer deleted);
}
