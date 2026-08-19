package com.xinsulu.repository;

import com.xinsulu.entity.FieldMappingRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字段映射规则数据访问层接口
 *
 * @author xinsulu-team
 */
@Repository
public interface FieldMappingRuleRepository extends JpaRepository<FieldMappingRule, Long> {

    /**
     * 根据报表类型查询所有映射规则
     *
     * @param reportType 报表类型
     * @return 映射规则列表
     */
    List<FieldMappingRule> findByReportTypeOrderByPriorityAsc(String reportType);

    /**
     * 根据报表类型和是否激活查询
     *
     * @param reportType 报表类型
     * @param isActive   是否激活
     * @return 映射规则列表
     */
    List<FieldMappingRule> findByReportTypeAndIsActiveOrderByPriorityAsc(String reportType, Boolean isActive);

    /**
     * 根据目标字段编码查询
     *
     * @param targetFieldCode 目标字段编码
     * @return 映射规则对象
     */
    FieldMappingRule findByTargetFieldCode(String targetFieldCode);

    /**
     * 根据源字段名模糊查询
     *
     * @param sourceFieldName 源字段名关键词
     * @return 映射规则列表
     */
    List<FieldMappingRule> findBySourceFieldNameContainingIgnoreCase(String sourceFieldName);

    /**
     * 统计激活状态的映射规则数量
     *
     * @param reportType 报表类型
     * @return 数量
     */
    long countByReportTypeAndIsActive(String reportType, Boolean isActive);

    /**
     * 根据删除标记查询（分页）
     *
     * @param deleted  删除标记
     * @param pageable 分页参数
     * @return 规则分页列表
     */
    Page<FieldMappingRule> findByDeleted(Integer deleted, Pageable pageable);

    /**
     * 根据源字段或目标字段模糊查询（分页）
     *
     * @param sourceField  源字段关键词
     * @param targetField  目标字段关键词
     * @param deleted      删除标记
     * @param pageable     分页参数
     * @return 规则分页列表
     */
    Page<FieldMappingRule> findBySourceFieldNameContainingOrTargetFieldNameContainingAndDeleted(
            String sourceField, String targetField, Integer deleted, Pageable pageable);
}
