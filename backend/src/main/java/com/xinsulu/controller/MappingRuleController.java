package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.entity.FieldMappingRule;
import com.xinsulu.repository.FieldMappingRuleRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 字段映射规则控制器
 * 管理OCR识别字段到标准财务字段的映射规则
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/mapping-rules")
@Api(tags = "字段映射规则管理")
public class MappingRuleController {

    @Autowired
    private FieldMappingRuleRepository fieldMappingRuleRepository;

    /**
     * 获取字段映射规则列表
     *
     * @return 规则列表
     */
    @GetMapping
    @ApiOperation(value = "获取映射规则列表", notes = "分页查询所有字段映射规则")
    public ApiResponse<Page<FieldMappingRule>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String keyword) {
        log.info("查询映射规则列表：page={}, keyword={}", page, keyword);

        Sort sort = Sort.by(Sort.Direction.ASC, "sourceFieldName");
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);

        Page<FieldMappingRule> rules;
        if (keyword != null && !keyword.trim().isEmpty()) {
            rules = fieldMappingRuleRepository.findBySourceFieldNameContainingOrTargetFieldNameContainingAndDeleted(
                    keyword, keyword, 0, pageRequest);
        } else {
            rules = fieldMappingRuleRepository.findByDeleted(0, pageRequest);
        }

        return ApiResponse.success(rules);
    }

    /**
     * 获取规则详情
     *
     * @param id 规则ID
     * @return 规则详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "规则详情", notes = "根据ID获取字段映射规则的详细信息")
    public ApiResponse<FieldMappingRule> detail(@PathVariable Long id) {
        log.info("查询规则详情：id={}", id);
        FieldMappingRule rule = fieldMappingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("映射规则不存在"));
        return ApiResponse.success(rule);
    }

    /**
     * 新增映射规则
     *
     * @param rule 映射规则
     * @return 创建后的规则
     */
    @PostMapping
    @ApiOperation(value = "新增映射规则", notes = "创建新的字段映射规则")
    public ApiResponse<FieldMappingRule> create(@Valid @RequestBody FieldMappingRule rule) {
        log.info("新增映射规则：source={}, target={}", rule.getSourceFieldName(), rule.getTargetFieldName());

        rule.setCreatedTime(LocalDateTime.now());
        rule.setUpdatedTime(LocalDateTime.now());
        rule.setDeleted(0);
        rule.setIsActive(true);

        FieldMappingRule savedRule = fieldMappingRuleRepository.save(rule);
        log.info("映射规则创建成功：id={}", savedRule.getId());

        return ApiResponse.success(savedRule);
    }

    /**
     * 编辑映射规则
     *
     * @param id   规则ID
     * @param rule 映射规则
     * @return 更新后的规则
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "编辑映射规则", notes = "更新字段映射规则的配置")
    public ApiResponse<FieldMappingRule> update(@PathVariable Long id,
                                                 @Valid @RequestBody FieldMappingRule rule) {
        log.info("编辑映射规则：id={}", id);

        FieldMappingRule existingRule = fieldMappingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("映射规则不存在"));

        // 更新字段
        if (rule.getSourceFieldName() != null) existingRule.setSourceFieldName(rule.getSourceFieldName());
        if (rule.getTargetFieldName() != null) existingRule.setTargetFieldName(rule.getTargetFieldName());
        if (rule.getTargetFieldCode() != null) existingRule.setTargetFieldCode(rule.getTargetFieldCode());
        if (rule.getMappingType() != null) existingRule.setMappingType(rule.getMappingType());
        if (rule.getDescription() != null) existingRule.setDescription(rule.getDescription());
        if (rule.getIsActive() != null) existingRule.setIsActive(rule.getIsActive());

        existingRule.setUpdatedTime(LocalDateTime.now());
        FieldMappingRule updatedRule = fieldMappingRuleRepository.save(existingRule);

        log.info("映射规则更新成功：id={}", id);
        return ApiResponse.success(updatedRule);
    }

    /**
     * 删除映射规则（软删除）
     *
     * @param id 规则ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除映射规则", notes = "软删除指定的字段映射规则")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除映射规则：id={}", id);

        FieldMappingRule rule = fieldMappingRuleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("映射规则不存在"));

        rule.setDeleted(1);
        rule.setUpdatedTime(LocalDateTime.now());
        fieldMappingRuleRepository.save(rule);

        log.info("映射规则删除成功：id={}", id);
        return ApiResponse.success();
    }

    /**
     * 批量导入映射规则
     *
     * @param rules 规则列表
     * @return 导入结果
     */
    @PostMapping("/batch-import")
    @ApiOperation(value = "批量导入", notes = "批量导入字段映射规则")
    public ApiResponse<Map<String, Object>> batchImport(@RequestBody List<FieldMappingRule> rules) {
        log.info("批量导入映射规则：count={}", rules.size());

        int successCount = 0;
        int failCount = 0;
        List<String> failedItems = new java.util.ArrayList<>();

        for (FieldMappingRule rule : rules) {
            try {
                rule.setId(null); // 确保新增
                rule.setCreatedTime(LocalDateTime.now());
                rule.setUpdatedTime(LocalDateTime.now());
                rule.setDeleted(0);
                rule.setIsActive(true);
                fieldMappingRuleRepository.save(rule);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failedItems.add(rule.getSourceFieldName() + " -> " + rule.getTargetFieldName()
                        + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("totalCount", rules.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failedItems", failedItems);

        log.info("批量导入完成：成功{}, 失败{}", successCount, failCount);
        return ApiResponse.success(result);
    }
}
