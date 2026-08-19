package com.xinsulu.repository;

import com.xinsulu.entity.Enterprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 企业数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface EnterpriseRepository extends JpaRepository<Enterprise, Long>, JpaSpecificationExecutor<Enterprise> {

    /**
     * 根据统一社会信用代码查找企业
     *
     * @param creditCode 统一社会信用代码
     * @return 企业对象
     */
    Optional<Enterprise> findByEnterpriseCode(String enterpriseCode);

    Optional<Enterprise> findByEnterpriseCodeIgnoreCase(String enterpriseCode);

    /**
     * 根据企业名称模糊查询（分页）
     *
     * @param name    企业名称关键词
     * @param pageable 分页参数
     * @return 企业分页列表
     */
    Page<Enterprise> findByEnterpriseNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * 根据行业分类查询企业列表
     *
     * @param industry 行业分类
     * @return 企业列表
     */
    List<Enterprise> findByIndustry(String industry);

    /**
     * 统计企业总数
     *
     * @return 企业数量
     */
    long countByDeleted(Integer deleted);

    /**
     * 查询最近创建的企业
     *
     * @param limit 限制数量
     * @return 企业列表
     */
    @Query("SELECT e FROM Enterprise e WHERE e.deleted = 0 ORDER BY e.createdTime DESC")
    List<Enterprise> findRecentEnterprises(Pageable pageable);

    /**
     * 根据风险等级列表统计企业数
     *
     * @param riskLevels 风险等级列表
     * @param deleted    删除标记
     * @return 企业数量
     */
    long countByRiskLevelInAndDeleted(List<String> riskLevels, Integer deleted);

    /**
     * 根据风险等级统计企业数
     *
     * @param riskLevel 风险等级
     * @param deleted   删除标记
     * @return 企业数量
     */
    long countByRiskLevelAndDeleted(String riskLevel, Integer deleted);

    @Query("SELECT COALESCE(AVG(e.healthScore), 0) FROM Enterprise e WHERE e.deleted = 0")
    Double findAverageHealthScore();
}
