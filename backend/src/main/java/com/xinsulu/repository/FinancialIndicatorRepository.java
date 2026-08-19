package com.xinsulu.repository;

import com.xinsulu.entity.FinancialIndicator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 财务指标定义数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FinancialIndicatorRepository extends JpaRepository<FinancialIndicator, Long> {

    /**
     * 根据指标编码查询
     *
     * @param indicatorCode 指标编码
     * @return 指标定义对象
     */
    FinancialIndicator findByIndicatorCode(String indicatorCode);

    /**
     * 根据分类查询指标列表
     *
     * @param category 分类（偿债能力/盈利能力等）
     * @return 指标列表
     */
    List<FinancialIndicator> findByCategoryOrderBySortOrderAsc(String category);

    /**
     * 根据维度查询指标列表
     *
     * @param dimension 维度
     * @return 指标列表
     */
    List<FinancialIndicator> findByDimensionOrderBySortOrderAsc(String dimension);

    /**
     * 查询所有激活的指标
     *
     * @return 激活的指标列表
     */
    List<FinancialIndicator> findByIsActiveTrueOrderBySortOrderAsc();

    /**
     * 根据分类和激活状态查询
     *
     * @param category 分类
     * @param isActive  是否激活
     * @return 指标列表
     */
    List<FinancialIndicator> findByCategoryAndIsActiveOrderBySortOrderAsc(String category, Boolean isActive);
}
