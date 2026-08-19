package com.xinsulu.repository;

import com.xinsulu.entity.BalanceSheetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资产负债表明细项数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface BalanceSheetItemRepository extends JpaRepository<BalanceSheetItem, Long> {

    /**
     * 根据资产负债表ID查询所有明细项
     *
     * @param statementId 资产负债表ID
     * @return 明细项列表
     */
    List<BalanceSheetItem> findByBalanceSheetIdOrderBySortOrderAsc(Long balanceSheetId);

    List<BalanceSheetItem> findByBalanceSheetIdAndItemCode(Long balanceSheetId, String itemCode);

    List<BalanceSheetItem> findByBalanceSheetIdAndParentItemCodeOrderBySortOrderAsc(
            Long balanceSheetId, String parentItemCode);

    void deleteByBalanceSheetId(Long balanceSheetId);

    long countByBalanceSheetId(Long balanceSheetId);

    List<BalanceSheetItem> findByBalanceSheetId(Long balanceSheetId);
}
