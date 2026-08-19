package com.xinsulu.repository;

import com.xinsulu.entity.IncomeStatementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 利润表明细项数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface IncomeStatementItemRepository extends JpaRepository<IncomeStatementItem, Long> {

    /**
     * 根据利润表ID查询所有明细项
     *
     * @param statementId 利润表ID
     * @return 明细项列表
     */
    List<IncomeStatementItem> findByIncomeStatementIdOrderBySortOrderAsc(Long incomeStatementId);

    List<IncomeStatementItem> findByIncomeStatementIdAndItemCode(Long incomeStatementId, String itemCode);

    List<IncomeStatementItem> findByIncomeStatementIdAndParentItemCodeOrderBySortOrderAsc(
            Long incomeStatementId, String parentItemCode);

    void deleteByIncomeStatementId(Long incomeStatementId);

    long countByIncomeStatementId(Long incomeStatementId);

    /**
     * 根据利润表ID查询（别名方法）
     *
     * @param incomeStatementId 利润表ID
     * @return 明细项列表
     */
    List<IncomeStatementItem> findByIncomeStatementId(Long incomeStatementId);
}
