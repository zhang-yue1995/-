package com.xinsulu.repository;

import com.xinsulu.entity.CashFlowStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 现金流量表数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface CashFlowStatementRepository extends JpaRepository<CashFlowStatement, Long> {

    /**
     * 根据企业和报告期查询现金流量表
     *
     * @param enterpriseId 企业ID
     * @param reportDate   报告日期
     * @return 现金流量表对象
     */
    Optional<CashFlowStatement> findByEnterpriseIdAndReportDate(Long enterpriseId, LocalDate reportDate);

    /**
     * 根据企业ID查询现金流量表列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 现金流量表分页列表
     */
    Page<CashFlowStatement> findByEnterpriseIdOrderByReportDateDesc(Long enterpriseId, Pageable pageable);

    /**
     * 根据归档ID查询
     *
     * @param archiveId 归档ID
     * @return 现金流量表对象
     */
    Optional<CashFlowStatement> findByArchiveId(Long archiveId);

    /**
     * 根据企业ID和日期范围查询
     *
     * @param enterpriseId 企业ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 现金流量表列表
     */
    List<CashFlowStatement> findByEnterpriseIdAndReportDateBetweenOrderByReportDateDesc(
            Long enterpriseId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询最新的现金流量表
     *
     * @param enterpriseId 企业ID
     * @return 最新现金流量表
     */
    Optional<CashFlowStatement> findFirstByEnterpriseIdOrderByReportDateDesc(Long enterpriseId);
}
