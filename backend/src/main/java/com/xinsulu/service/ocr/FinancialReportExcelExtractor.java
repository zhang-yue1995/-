package com.xinsulu.service.ocr;

import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从异构 XLS/XLSX 财务报表中提取标准字段。
 *
 * <p>解析依据是工作表语义、字段别名及金额列标题，而不是固定单元格坐标。
 * 因此可以兼容只包含一张或两张报表、左右分栏资产负债表以及不同时期列名。
 * 未出现的字段和整张缺失报表由 {@link FinancialReportFieldNormalizer} 补为空值。</p>
 */
@Slf4j
@Component
public class FinancialReportExcelExtractor {

    private static final Pattern ENTERPRISE = Pattern.compile(
            "(?:编制单位|企业名称)\\s*[:：]\\s*([^\\s]+)");
    private static final Pattern DATE = Pattern.compile(
            "(20\\d{2})\\s*[年./-]\\s*(\\d{1,2})\\s*[月./-]\\s*(\\d{1,2})\\s*日?");
    private static final Pattern UNIT = Pattern.compile("单位\\s*[:：]\\s*([^\\s]+)");

    private final DataFormatter formatter = new DataFormatter(Locale.CHINA);

    public List<OcrFieldResult> extract(OcrTask task, File file) throws Exception {
        Map<String, OcrFieldResult> extracted = new LinkedHashMap<>();
        int recognizedSheets = 0;
        // 通过只读输入流打开，关闭 Workbook 时不会回写用户上传的原始文件。
        try (InputStream inputStream = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String fieldType = detectSheetType(sheet, evaluator);
                if (fieldType == null) {
                    continue;
                }
                recognizedSheets++;
                extractMetadata(task, sheet, evaluator);
                extractSheet(task, sheet, fieldType, evaluator, extracted);
            }
        }

        if (recognizedSheets == 0 || extracted.isEmpty()) {
            throw new IllegalArgumentException("Excel 中未检测到可识别的资产负债表、利润表或现金流量表");
        }
        log.info("Excel 本地解析完成: taskId={}, sheets={}, matchedFields={}",
                task.getId(), recognizedSheets, extracted.size());
        return new ArrayList<>(extracted.values());
    }

    private void extractSheet(OcrTask task, Sheet sheet, String fieldType,
                              FormulaEvaluator evaluator,
                              Map<String, OcrFieldResult> extracted) {
        List<HeaderColumn> headers = findAmountHeaders(sheet, fieldType, evaluator);
        if (headers.isEmpty()) {
            log.warn("工作表未检测到金额列标题: sheet={}", sheet.getSheetName());
            return;
        }

        Map<String, FinancialReportFieldTemplates.FieldDefinition> aliases = aliasIndex(fieldType);
        int firstRow = Math.max(0, headers.get(0).rowIndex + 1);
        for (int rowIndex = firstRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            for (int column = 0; column < row.getLastCellNum(); column++) {
                String rawLabel = cellText(row.getCell(column), evaluator);
                if (rawLabel.isEmpty()) continue;
                FinancialReportFieldTemplates.FieldDefinition definition = aliases.get(compactLabel(rawLabel));
                if (definition == null) continue;

                String primary = amountFor(row, column, headers, AmountRole.PRIMARY, evaluator);
                String secondary = amountFor(row, column, headers, AmountRole.SECONDARY, evaluator);
                String tertiary = amountFor(row, column, headers, AmountRole.TERTIARY, evaluator);
                OcrFieldResult field = buildField(task, definition, primary, secondary, tertiary);

                OcrFieldResult existing = extracted.get(definition.getCode());
                if (existing == null || shouldReplace(rawLabel, existing)) {
                    extracted.put(definition.getCode(), field);
                }
            }
        }
    }

    private List<HeaderColumn> findAmountHeaders(Sheet sheet, String fieldType,
                                                  FormulaEvaluator evaluator) {
        List<HeaderColumn> best = Collections.emptyList();
        int lastHeaderCandidate = Math.min(sheet.getLastRowNum(), 15);
        for (int rowIndex = 0; rowIndex <= lastHeaderCandidate; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            List<HeaderColumn> current = new ArrayList<>();
            for (int column = 0; column < row.getLastCellNum(); column++) {
                AmountRole role = amountRole(cellText(row.getCell(column), evaluator), fieldType);
                if (role != null) {
                    current.add(new HeaderColumn(rowIndex, column, role));
                }
            }
            if (current.size() > best.size()) {
                best = current;
            }
        }
        return best;
    }

    private AmountRole amountRole(String header, String fieldType) {
        String compact = compactLabel(header);
        if (compact.isEmpty()) return null;
        if (FinancialReportFieldTemplates.BALANCE_SHEET.equals(fieldType)) {
            if (compact.contains("期末") || compact.contains("年末")) return AmountRole.PRIMARY;
            if (compact.contains("年初") || compact.contains("期初")) return AmountRole.SECONDARY;
            return null;
        }
        if (compact.contains("本月") || compact.contains("当月")) return AmountRole.TERTIARY;
        if (compact.contains("上期") || compact.contains("上年同期")
                || compact.contains("上年累计") || compact.contains("上年实际")) {
            return AmountRole.SECONDARY;
        }
        if (compact.contains("本年累计") || compact.contains("本期")
                || compact.contains("本年实际") || compact.contains("累计金额")) {
            return AmountRole.PRIMARY;
        }
        return null;
    }

    private String amountFor(Row row, int labelColumn, List<HeaderColumn> headers,
                             AmountRole role, FormulaEvaluator evaluator) {
        HeaderColumn nearest = null;
        int distance = Integer.MAX_VALUE;
        for (HeaderColumn header : headers) {
            int currentDistance = header.columnIndex - labelColumn;
            if (header.role == role && currentDistance > 0 && currentDistance <= 6
                    && currentDistance < distance) {
                nearest = header;
                distance = currentDistance;
            }
        }
        return nearest == null ? "" : moneyValue(row.getCell(nearest.columnIndex), evaluator);
    }

    private Map<String, FinancialReportFieldTemplates.FieldDefinition> aliasIndex(String fieldType) {
        Map<String, FinancialReportFieldTemplates.FieldDefinition> result = new LinkedHashMap<>();
        for (FinancialReportFieldTemplates.FieldDefinition definition
                : FinancialReportFieldTemplates.forType(fieldType)) {
            for (String alias : definition.getAliases()) {
                result.putIfAbsent(compactLabel(alias), definition);
            }
        }
        return result;
    }

    private String detectSheetType(Sheet sheet, FormulaEvaluator evaluator) {
        String probe = sheet.getSheetName() + " " + firstRowsText(sheet, evaluator, 8);
        String compact = compactLabel(probe);
        if (compact.contains("资产负债表") || (compact.contains("资产") && compact.contains("负债"))) {
            return FinancialReportFieldTemplates.BALANCE_SHEET;
        }
        if (compact.contains("利润表") || compact.contains("损益表")) {
            return FinancialReportFieldTemplates.INCOME_STATEMENT;
        }
        if (compact.contains("现金流量表")) {
            return FinancialReportFieldTemplates.CASH_FLOW_STATEMENT;
        }
        return null;
    }

    private void extractMetadata(OcrTask task, Sheet sheet, FormulaEvaluator evaluator) {
        String text = firstRowsText(sheet, evaluator, 8);
        if (task.getSourceEnterpriseName() == null || task.getSourceEnterpriseName().trim().isEmpty()) {
            Matcher enterprise = ENTERPRISE.matcher(text);
            if (enterprise.find()) task.setSourceEnterpriseName(enterprise.group(1).trim());
        }
        if (task.getSourceReportDate() == null) {
            Matcher date = DATE.matcher(text);
            if (date.find()) {
                int year = Integer.parseInt(date.group(1));
                int month = Integer.parseInt(date.group(2));
                int day = Integer.parseInt(date.group(3));
                task.setSourceReportDate(LocalDate.of(year, month, day));
                task.setSourceReportPeriod(String.format("%04d-%02d", year, month));
            }
        }
        if (task.getSourceUnit() == null || task.getSourceUnit().trim().isEmpty()) {
            Matcher unit = UNIT.matcher(text);
            if (unit.find()) task.setSourceUnit(unit.group(1).contains("万") ? "万元" : "元");
        }
    }

    private String firstRowsText(Sheet sheet, FormulaEvaluator evaluator, int rows) {
        StringBuilder text = new StringBuilder();
        int last = Math.min(sheet.getLastRowNum(), rows - 1);
        for (int rowIndex = 0; rowIndex <= last; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;
            for (int column = 0; column < row.getLastCellNum(); column++) {
                String value = cellText(row.getCell(column), evaluator);
                if (!value.isEmpty()) text.append(value).append(' ');
            }
        }
        return text.toString();
    }

    private String cellText(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        try {
            return formatter.formatCellValue(cell, evaluator).replace("_x000D_", " ").trim();
        } catch (RuntimeException exception) {
            return formatter.formatCellValue(cell).replace("_x000D_", " ").trim();
        }
    }

    private String moneyValue(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        try {
            CellType type = cell.getCellType();
            double number;
            if (type == CellType.NUMERIC) {
                number = cell.getNumericCellValue();
                return decimal(number);
            }
            if (type == CellType.FORMULA) {
                CellValue evaluated = evaluator.evaluate(cell);
                if (evaluated != null && evaluated.getCellType() == CellType.NUMERIC) {
                    return decimal(evaluated.getNumberValue());
                }
            }
        } catch (RuntimeException ignored) {
            // 回退到格式化文本解析。
        }
        String value = cellText(cell, evaluator).trim();
        if (value.isEmpty() || "-".equals(value) || "—".equals(value)) return "";
        boolean negative = value.startsWith("(") && value.endsWith(")");
        value = value.replace(",", "").replace("，", "")
                .replace("￥", "").replace("¥", "")
                .replace("(", "").replace(")", "").trim();
        if (!value.matches("[-+]?\\d+(?:\\.\\d+)?")) return "";
        return (negative ? "-" : "") + new BigDecimal(value).stripTrailingZeros().toPlainString();
    }

    private String decimal(double value) {
        if (!Double.isFinite(value)) return "";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private String compactLabel(String value) {
        if (value == null) return "";
        String compact = value.replace("_x000D_", " ")
                .replaceAll("[\\r\\n]", "")
                .replaceAll("^[一二三四五六七八九十]+[、.．]", "")
                .replaceAll("^\\d+[、.．]", "")
                .replaceAll("^(加|减)\\s*[:：]?", "")
                .replaceAll("[（(][^）)]*[）)]", "")
                .replaceAll("[\\s:：,，、()（）\\[\\]‘’“”\"'－—-]", "")
                .toLowerCase(Locale.ROOT);
        return compact.trim();
    }

    private boolean shouldReplace(String rawLabel, OcrFieldResult existing) {
        return compactLabel(rawLabel).contains("净额")
                && !compactLabel(existing.getFieldName()).contains("净额");
    }

    private OcrFieldResult buildField(OcrTask task,
                                      FinancialReportFieldTemplates.FieldDefinition definition,
                                      String primary, String secondary, String tertiary) {
        OcrFieldResult field = new OcrFieldResult();
        field.setOcrTask(task);
        field.setFieldCode(definition.getCode());
        field.setFieldName(definition.getName());
        field.setFieldValue(primary);
        field.setSecondaryValue(secondary);
        field.setTertiaryValue(tertiary);
        field.setConfidenceScore(new BigDecimal("99.0000"));
        field.setConfidenceLevel("HIGH");
        field.setFieldType(definition.getFieldType());
        field.setPageNumber(definition.getPageNumber());
        field.setIsReviewed(0);
        field.setCreatedTime(LocalDateTime.now());
        field.setUpdatedTime(LocalDateTime.now());
        field.setDeleted(0);
        return field;
    }

    private enum AmountRole { PRIMARY, SECONDARY, TERTIARY }

    private static final class HeaderColumn {
        private final int rowIndex;
        private final int columnIndex;
        private final AmountRole role;

        private HeaderColumn(int rowIndex, int columnIndex, AmountRole role) {
            this.rowIndex = rowIndex;
            this.columnIndex = columnIndex;
            this.role = role;
        }
    }
}
