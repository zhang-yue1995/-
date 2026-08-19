package com.xinsulu.service;

import com.xinsulu.entity.FieldMappingRule;
import java.util.List;
import java.util.Map;

/**
 * 字段映射服务接口
 * 用于管理OCR字段与标准财务字段的映射关系
 *
 * @author xinsulu-team
 */
public interface FieldMappingService {

    /**
     * 根据报表类型获取所有映射规则
     *
     * @param reportType 报表类型
     * @return 映射规则列表
     */
    List<FieldMappingRule> getMappingsByReportType(String reportType);

    /**
     * 执行字段映射转换
     *
     * @param ocrFields   OCR原始字段（字段名->值）
     * @param reportType  报表类型
     * @return 映射后的标准字段（标准编码->值）
     */
    Map<String, Object> mapFields(Map<String, Object> ocrFields, String reportType);

    /**
     * 添加或更新映射规则
     *
     * @param rule 映射规则
     * @return 更新后的规则
     */
    FieldMappingRule saveRule(FieldMappingRule rule);

    /**
     * 删除映射规则
     *
     * @param ruleId 规则ID
     */
    void deleteRule(Long ruleId);

    /**
     * 初始化默认映射规则
     */
    void initDefaultRules();
}
