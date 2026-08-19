package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.entity.HealthWeightConfig;
import com.xinsulu.entity.IndicatorRuleConfig;
import com.xinsulu.repository.HealthWeightConfigRepository;
import com.xinsulu.repository.IndicatorRuleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/indicator-rules")
@RequiredArgsConstructor
public class IndicatorRuleConfigController {
    private final IndicatorRuleConfigRepository ruleRepository;
    private final HealthWeightConfigRepository weightRepository;

    @GetMapping
    public ApiResponse<Page<IndicatorRuleConfig>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String status) {
        Specification<IndicatorRuleConfig> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("indicatorName"), pattern),
                        cb.like(root.get("formula"), pattern)));
            }
            if (StringUtils.hasText(industry)) {
                predicates.add(cb.or(
                        cb.equal(root.get("applicableIndustry"), industry),
                        cb.equal(root.get("applicableIndustry"), "全部")));
            }
            if ("enabled".equals(status)) {
                predicates.add(cb.isTrue(root.get("isEnabled")));
            } else if ("disabled".equals(status)) {
                predicates.add(cb.isFalse(root.get("isEnabled")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.ASC, "id"));
        return ApiResponse.success(ruleRepository.findAll(specification, pageable));
    }

    @PostMapping
    public ApiResponse<IndicatorRuleConfig> create(@RequestBody IndicatorRuleConfig request) {
        if (!StringUtils.hasText(request.getIndicatorName()) || !StringUtils.hasText(request.getFormula())) {
            throw new BusinessException("指标名称和计算公式不能为空");
        }
        if (!StringUtils.hasText(request.getIndicatorCode())) {
            request.setIndicatorCode("custom_" + System.currentTimeMillis());
        }
        if (!StringUtils.hasText(request.getThresholdDirection())) {
            request.setThresholdDirection(request.getNormalThreshold() != null
                    && (request.getNormalThreshold().contains("≤")
                    || request.getNormalThreshold().contains("<")) ? "AT_MOST" : "AT_LEAST");
        }
        if (request.getNormalThresholdValue() == null) {
            request.setNormalThresholdValue(parseThreshold(
                    request.getNormalThreshold(), request.getThresholdDirection(), false, null));
        }
        if (request.getAttentionThresholdValue() == null) {
            request.setAttentionThresholdValue(parseThreshold(
                    request.getAttentionThreshold(), request.getThresholdDirection(), true, null));
        }
        request.setId(null);
        request.setCreatedTime(LocalDateTime.now());
        request.setUpdatedTime(LocalDateTime.now());
        request.setDeleted(0);
        request.setIsEnabled(request.getIsEnabled() == null || request.getIsEnabled());
        return ApiResponse.success(ruleRepository.save(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<IndicatorRuleConfig> update(
            @PathVariable Long id, @RequestBody IndicatorRuleConfig request) {
        IndicatorRuleConfig target = findRule(id);
        if (StringUtils.hasText(request.getIndicatorName())) target.setIndicatorName(request.getIndicatorName());
        if (StringUtils.hasText(request.getFormula())) target.setFormula(request.getFormula());
        if (request.getNormalThreshold() != null) {
            target.setNormalThreshold(request.getNormalThreshold());
            target.setNormalThresholdValue(parseThreshold(request.getNormalThreshold(),
                    target.getThresholdDirection(), false, target.getNormalThresholdValue()));
        }
        if (request.getAttentionThreshold() != null) {
            target.setAttentionThreshold(request.getAttentionThreshold());
            target.setAttentionThresholdValue(parseThreshold(request.getAttentionThreshold(),
                    target.getThresholdDirection(), true, target.getAttentionThresholdValue()));
        }
        if (request.getHighRiskThreshold() != null) target.setHighRiskThreshold(request.getHighRiskThreshold());
        if (request.getNormalThresholdValue() != null) {
            target.setNormalThresholdValue(request.getNormalThresholdValue());
        }
        if (request.getAttentionThresholdValue() != null) {
            target.setAttentionThresholdValue(request.getAttentionThresholdValue());
        }
        if (request.getThresholdDirection() != null) target.setThresholdDirection(request.getThresholdDirection());
        if (request.getWeight() != null) target.setWeight(request.getWeight());
        if (request.getApplicableIndustry() != null) {
            target.setApplicableIndustry(request.getApplicableIndustry());
        }
        if (request.getIsEnabled() != null) target.setIsEnabled(request.getIsEnabled());
        if (request.getRemark() != null) target.setRemark(request.getRemark());
        target.setUpdatedTime(LocalDateTime.now());
        return ApiResponse.success(ruleRepository.save(target));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        IndicatorRuleConfig target = findRule(id);
        target.setDeleted(1);
        target.setUpdatedTime(LocalDateTime.now());
        ruleRepository.save(target);
        return ApiResponse.success();
    }

    @GetMapping("/weights")
    public ApiResponse<List<HealthWeightConfig>> getWeights() {
        return ApiResponse.success(weightRepository.findAllByOrderByIdAsc());
    }

    @PutMapping("/weights")
    public ApiResponse<List<HealthWeightConfig>> updateWeights(
            @RequestBody List<HealthWeightConfig> request) {
        int total = request.stream().map(HealthWeightConfig::getWeight)
                .filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        if (total != 100) {
            throw new BusinessException("五维权重之和必须等于100%");
        }
        for (HealthWeightConfig incoming : request) {
            HealthWeightConfig target = weightRepository.findByDimensionCode(incoming.getDimensionCode())
                    .orElseThrow(() -> new BusinessException("未知评分维度：" + incoming.getDimensionCode()));
            target.setWeight(incoming.getWeight());
            target.setUpdatedTime(LocalDateTime.now());
            weightRepository.save(target);
        }
        return ApiResponse.success(weightRepository.findAllByOrderByIdAsc());
    }

    private IndicatorRuleConfig findRule(Long id) {
        return ruleRepository.findById(id)
                .filter(rule -> Integer.valueOf(0).equals(rule.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "指标规则不存在"));
    }

    private BigDecimal parseThreshold(String text, String direction, boolean attention,
                                      BigDecimal existingValue) {
        if (!StringUtils.hasText(text)) {
            return existingValue;
        }
        Matcher matcher = Pattern.compile("-?\\d+(?:\\.\\d+)?").matcher(text);
        List<BigDecimal> values = new ArrayList<>();
        while (matcher.find()) {
            values.add(new BigDecimal(matcher.group()));
        }
        if (values.isEmpty()) {
            return existingValue;
        }
        BigDecimal selected = attention && "AT_MOST".equals(direction)
                ? values.get(values.size() - 1) : values.get(0);
        if (text.contains("%") && existingValue != null
                && existingValue.abs().compareTo(BigDecimal.ONE) < 0
                && selected.abs().compareTo(BigDecimal.ONE) >= 0) {
            selected = selected.divide(new BigDecimal("100"));
        }
        return selected;
    }
}
