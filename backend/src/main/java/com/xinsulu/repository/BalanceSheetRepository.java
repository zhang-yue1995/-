package com.xinsulu.repository;

import com.xinsulu.entity.BalanceSheet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 资产负债表数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface BalanceSheetRepository extends JpaRepository<BalanceSheet, Long> {

    /**
     * 根据企业和报告期查询资产负债表
     *
     * @param enterpriseId 企业ID
     * @param reportDate   报告日期
     * @return 资产负债表对象
     */
    Optional<BalanceSheet> findByEnterpriseIdAndReportDate(Long enterpriseId, LocalDate reportDate);

    /**
     * 根据企业ID查询资产负债表列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 资产负债表分页列表
     */
    Page<BalanceSheet> findByEnterpriseIdOrderByReportDateDesc(Long enterpriseId, Pageable pageable);

    /**
     * 根据归档ID查询
     *
     * @param archiveId 归档ID
     * @return 资产负债表对象
     */
    Optional<BalanceSheet> findByArchiveId(Long archiveId);

    /**
     * 根据企业ID和日期范围查询
     *
     * @param enterpriseId 企业ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 资产负债表列表
     */
    List<BalanceSheet> findByEnterpriseIdAndReportDateBetweenOrderByReportDateDesc(
            Long enterpriseId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询最新的资产负债表
     *
     * @param enterpriseId 企业ID
     * @return 最新资产负债表
     */
    Optional<BalanceSheet> findFirstByEnterpriseIdOrderByReportDateDesc(Long enterpriseId);
}
