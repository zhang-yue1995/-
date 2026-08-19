package com.xinsulu.repository;

import com.xinsulu.entity.FinancialIndicatorValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 财务指标值数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FinancialIndicatorValueRepository extends JpaRepository<FinancialIndicatorValue, Long> {

    /**
     * 根据报表ID查询所有指标值
     *
     * @param reportId 报表ID
     * @return 指标值列表
     */
    List<FinancialIndicatorValue> findByReportId(Long reportId);

    /**
     * 根据企业和报表ID查询指标值
     *
     * @param enterpriseId 企业ID
     * @param reportId     报表ID
     * @return 指标值列表
     */
    List<FinancialIndicatorValue> findByEnterpriseIdAndReportId(Long enterpriseId, Long reportId);

    /**
     * 根据企业和指标编码查询最新值
     *
     * @param enterpriseId  企业ID
     * @param indicatorCode 指标编码
     * @return 指标值列表（按时间倒序）
     */
    List<FinancialIndicatorValue> findByEnterpriseIdAndIndicatorCodeOrderByCalculatedTimeDesc(
            Long enterpriseId, String indicatorCode);

    /**
     * 根据分类查询企业的指标值
     *
     * @param enterpriseId 企业ID
     * @param reportId     报表ID
     * @param category     分类
     * @return 指标值列表
     */
    List<FinancialIndicatorValue> findByEnterpriseIdAndReportIdAndCategory(Long enterpriseId, Long reportId, String category);

    /**
     * 根据状态查询指标值
     *
     * @param status   状态（normal/warning/danger）
     * @param pageable 分页参数
     * @return 指标值分页列表
     */
    Page<FinancialIndicatorValue>findByStatusOrderByCalculatedTimeDesc(String status, Pageable pageable);

    /**
     * 删除指定报表的所有指标值
     *
     * @param reportId 报表ID
     */
    void deleteByReportId(Long reportId);

    /**
     * 根据报表归档ID和指标编码查询指标值
     *
     * @param archiveId     报表归档ID
     * @param indicatorCode 指标编码
     * @return 指标值对象
     */
    Optional<FinancialIndicatorValue> findFirstByReportIdAndIndicatorCodeAndDeletedOrderByIdDesc(
            Long reportId, String indicatorCode, Integer deleted);
}
