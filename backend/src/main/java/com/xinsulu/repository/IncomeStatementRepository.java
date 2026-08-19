package com.xinsulu.repository;

import com.xinsulu.entity.IncomeStatement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 利润表数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface IncomeStatementRepository extends JpaRepository<IncomeStatement, Long> {

    /**
     * 根据企业和报告期查询利润表
     *
     * @param enterpriseId 企业ID
     * @param reportDate   报告日期
     * @return 利润表对象
     */
    Optional<IncomeStatement> findByEnterpriseIdAndEndDate(Long enterpriseId, LocalDate reportDate);

    /**
     * 根据企业ID查询利润表列表（分页）
     *
     * @param enterpriseId 企业ID
     * @param pageable     分页参数
     * @return 利润表分页列表
     */
    Page<IncomeStatement> findByEnterpriseIdOrderByEndDateDesc(Long enterpriseId, Pageable pageable);

    /**
     * 根据归档ID查询
     *
     * @param archiveId 归档ID
     * @return 利润表对象
     */
    Optional<IncomeStatement> findByArchiveId(Long archiveId);

    /**
     * 根据企业ID和日期范围查询
     *
     * @param enterpriseId 企业ID
     * @param startDate    开始日期
     * @param endDate      结束日期
     * @return 利润表列表
     */
    List<IncomeStatement> findByEnterpriseIdAndEndDateBetweenOrderByEndDateDesc(
            Long enterpriseId, LocalDate startDate, LocalDate endDate);

    /**
     * 查询最新的利润表
     *
     * @param enterpriseId 企业ID
     * @return 最新利润表
     */
    Optional<IncomeStatement> findFirstByEnterpriseIdOrderByEndDateDesc(Long enterpriseId);
}
