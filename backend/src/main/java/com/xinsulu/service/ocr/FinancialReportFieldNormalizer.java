package com.xinsulu.service.ocr;

import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 将任意 OCR 返回值映射并补齐到标准 140 字段。 */
@Component
public class FinancialReportFieldNormalizer {

    public List<OcrFieldResult> normalize(OcrTask task, List<OcrFieldResult> source) {
        List<OcrFieldResult> normalized = new ArrayList<>();
        List<OcrFieldResult> safeSource = source == null ? new ArrayList<>() : source;
        Set<Integer> consumed = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (FinancialReportFieldTemplates.FieldDefinition definition : FinancialReportFieldTemplates.all()) {
            OcrFieldResult matched = findMatch(definition, safeSource, consumed);
            OcrFieldResult target = matched == null ? new OcrFieldResult() : matched;
            target.setId(null);
            target.setOcrTask(task);
            target.setFieldCode(definition.getCode());
            target.setFieldName(definition.getName());
            target.setFieldType(definition.getFieldType());
            target.setPageNumber(definition.getPageNumber());
            target.setFieldValue(target.getFieldValue() == null ? "" : target.getFieldValue().trim());
            target.setSecondaryValue(target.getSecondaryValue() == null ? "" : target.getSecondaryValue().trim());
            target.setTertiaryValue(target.getTertiaryValue() == null ? "" : target.getTertiaryValue().trim());
            target.setConfidenceScore(target.getConfidenceScore() == null ? BigDecimal.ZERO : target.getConfidenceScore());
            target.setConfidenceLevel(level(target.getConfidenceScore()));
            target.setIsReviewed(target.getIsReviewed() == null ? 0 : target.getIsReviewed());
            target.setCreatedTime(target.getCreatedTime() == null ? now : target.getCreatedTime());
            target.setUpdatedTime(now);
            target.setDeleted(0);
            normalized.add(target);
        }
        return normalized;
    }

    private OcrFieldResult findMatch(FinancialReportFieldTemplates.FieldDefinition definition,
                                     List<OcrFieldResult> source, Set<Integer> consumed) {
        for (int i = 0; i < source.size(); i++) {
            OcrFieldResult candidate = source.get(i);
            if (!consumed.contains(i) && definition.getCode().equalsIgnoreCase(value(candidate.getFieldCode()))) {
                consumed.add(i);
                return candidate;
            }
        }
        for (int i = 0; i < source.size(); i++) {
            OcrFieldResult candidate = source.get(i);
            if (consumed.contains(i) || !sameType(definition.getFieldType(), candidate.getFieldType())) {
                continue;
            }
            String candidateName = compact(candidate.getFieldName());
            for (String alias : definition.getAliases()) {
                if (candidateName.equals(compact(alias))) {
                    consumed.add(i);
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean sameType(String expected, String actual) {
        if (expected.equalsIgnoreCase(value(actual))) return true;
        String compactType = value(actual).toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (FinancialReportFieldTemplates.BALANCE_SHEET.equals(expected)) {
            return compactType.contains("BALANCE") || compactType.contains("资产负债");
        }
        if (FinancialReportFieldTemplates.INCOME_STATEMENT.equals(expected)) {
            return compactType.contains("INCOME") || compactType.contains("PROFIT") || compactType.contains("利润");
        }
        return compactType.contains("CASH") || compactType.contains("现金流");
    }

    private String compact(String value) {
        return value(value).replaceAll("[\\s:：,，、()（）\\[\\]‘’“”\"']", "").toLowerCase(Locale.ROOT);
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String level(BigDecimal confidence) {
        if (confidence.compareTo(new BigDecimal("90")) >= 0) return "HIGH";
        if (confidence.compareTo(new BigDecimal("70")) >= 0) return "MEDIUM";
        return "LOW";
    }
}
