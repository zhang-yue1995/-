package com.xinsulu.repository;

import com.xinsulu.entity.CashFlowStatementItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 现金流量表明细项数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface CashFlowStatementItemRepository extends JpaRepository<CashFlowStatementItem, Long> {

    /**
     * 根据现金流量表ID查询所有明细项
     *
     * @param statementId 现金流量表ID
     * @return 明细项列表
     */
    List<CashFlowStatementItem> findByStatementIdOrderByRowNumber(Long statementId);

    /**
     * 根据现金流量表ID和项目编码查询
     *
     * @param statementId 现金流量表ID
     * @param itemCode    项目编码
     * @return 明细项列表
     */
    List<CashFlowStatementItem> findByStatementIdAndItemCode(Long statementId, String itemCode);

    /**
     * 根据现金流量表ID和项目类型查询
     *
     * @param statementId 现金流量表ID
     * @param itemType    项目类型（经营/投资/筹资）
     * @return 明细项列表
     */
    List<CashFlowStatementItem> findByStatementIdAndItemTypeOrderByRowNumber(Long statementId, String itemType);

    /**
     * 删除指定现金流量表的所有明细项
     *
     * @param statementId 现金流量表ID
     */
    void deleteByStatementId(Long statementId);

    /**
     * 统计现金流量表的明细项数量
     *
     * @param statementId 现金流量表ID
     * @return 数量
     */
    long countByStatementId(Long statementId);

    /**
     * 根据现金流量表ID查询（别名方法）
     *
     * @param statementId 现金流量表ID
     * @return 明细项列表
     */
    List<CashFlowStatementItem> findByStatementId(Long statementId);

    /**
     * 根据现金流量表ID查询（另一个别名方法）
     *
     * @param statementArchiveId 现金流量表ID
     * @return 明细项列表
     */
    List<CashFlowStatementItem> findByStatementArchiveId(Long statementArchiveId);
}
