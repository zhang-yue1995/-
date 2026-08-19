package com.xinsulu.repository;

import com.xinsulu.entity.HistoricalIndicatorValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 历史指标值数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface HistoricalIndicatorValueRepository extends JpaRepository<HistoricalIndicatorValue, Long> {

    /**
     * 根据企业和指标编码查询历史值（按日期倒序）
     *
     * @param enterpriseId  企业ID
     * @param indicatorCode 指标编码
     * @return 历史指标值列表
     */
    List<HistoricalIndicatorValue> findByEnterpriseIdAndIndicatorCodeOrderByReportDateAsc(
            Long enterpriseId, String indicatorCode);

    /**
     * 根据企业和指标编码及日期范围查询
     *
     * @param enterpriseId  企业ID
     * @param indicatorCode 指标编码
     * @param startDate     开始日期
     * @param endDate       结束日期
     * @return 历史指标值列表
     */
    List<HistoricalIndicatorValue> findByEnterpriseIdAndIndicatorCodeAndReportDateBetweenOrderByReportDateAsc(
            Long enterpriseId, String indicatorCode, LocalDate startDate, LocalDate endDate);

    /**
     * 根据企业和分类查询最新一期的所有指标值
     *
     * @param enterpriseId 企业ID
     * @param category     分类
     * @return 指标值列表
     */
    @Query("SELECT h FROM HistoricalIndicatorValue h WHERE h.enterprise.id = :enterpriseId " +
           "AND h.category = :category AND h.reportDate = " +
           "(SELECT MAX(h2.reportDate) FROM HistoricalIndicatorValue h2 " +
           "WHERE h2.enterprise.id = :enterpriseId AND h2.category = :category)")
    List<HistoricalIndicatorValue> findLatestByEnterpriseAndCategory(
            @Param("enterpriseId") Long enterpriseId, @Param("category") String category);

    /**
     * 根据企业查询最近N期的指标趋势数据
     *
     * @param enterpriseId 企业ID
     * @param indicatorCode 指标编码
     * @param pageable     分页参数
     * @return 历史指标值分页列表
     */
    Page<HistoricalIndicatorValue> findByEnterpriseIdAndIndicatorCodeOrderByReportDateDesc(
            Long enterpriseId, String indicatorCode, Pageable pageable);

    /**
     * 根据年份查询企业的所有指标值
     *
     * @param enterpriseId 企业ID
     * @param year         年份
     * @return 指标值列表
     */
    List<HistoricalIndicatorValue> findByEnterpriseIdAndYearOrderByReportDateAsc(Long enterpriseId, Integer year);

    /**
     * 删除指定企业的历史指标值
     *
     * @param enterpriseId 企业ID
     */
    void deleteByEnterpriseId(Long enterpriseId);
}
