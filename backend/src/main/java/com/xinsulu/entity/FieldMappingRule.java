package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 字段映射规则实体类
 * 用于定义OCR识别字段与标准财务报表字段的映射关系
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "field_mapping_rule")
public class FieldMappingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_field_name", nullable = false, length = 200)
    private String sourceFieldName;   // OCR原始字段名

    @Column(name = "target_field_code", nullable = false, length = 100)
    private String targetFieldCode;   // 标准字段编码

    @Column(name = "target_field_name", nullable = false, length = 200)
    private String targetFieldName;   // 标准字段名称

    @Column(name = "report_type", nullable = false, length = 20)
    private String reportType;        // balance_sheet/income_statement/cash_flow

    @Column(name = "mapping_type", length = 20)
    private String mappingType;       // direct/regex/fuzzy

    @Column(name = "priority")
    private Integer priority;         // 映射优先级

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    @Column(nullable = false)
    private Integer deleted;
}
