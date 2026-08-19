package com.xinsulu.service.ocr;

import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从带文字层的标准三表 PDF 中按原表列坐标提取表头元数据及完整金额列。 */
@Slf4j
@Component
public class FinancialReportPdfExtractor {

    private static final Pattern HEADER = Pattern.compile(
            "企业名称\\s*[:：]\\s*(.+?)\\s+(\\d{4}[-/.年]\\d{1,2}(?:[-/.月]\\d{1,2})?)");
    private static final Pattern UNIT = Pattern.compile("单位\\s*[:：]\\s*([^\\s]+)");
    private static final Pattern MONEY = Pattern.compile("[-−—]?\\s*\\d[\\d,，\\s]*\\.\\s*\\d{1,2}");

    public List<OcrFieldResult> extract(OcrTask task, File file) throws Exception {
        List<OcrFieldResult> fields = new ArrayList<>();
        int matchedLabels = 0;
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper fullStripper = new PDFTextStripper();
            fullStripper.setSortByPosition(true);
            String fullText = fullStripper.getText(document);
            extractMetadata(task, fullText);

            List<List<PositionedLine>> positionedPages = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                PositionedTextStripper pageStripper = new PositionedTextStripper();
                pageStripper.setSortByPosition(true);
                pageStripper.setStartPage(pageNumber);
                pageStripper.setEndPage(pageNumber);
                pageStripper.getText(document);
                positionedPages.add(pageStripper.lines());
            }

            for (FinancialReportFieldTemplates.FieldDefinition definition : FinancialReportFieldTemplates.all()) {
                if (definition.getPageNumber() > document.getNumberOfPages()) {
                    fields.add(buildField(task, definition, "", "", "", BigDecimal.ZERO));
                    continue;
                }
                ExtractedValue extracted = extractValue(
                        positionedPages.get(definition.getPageNumber() - 1),
                        definition,
                        document.getPage(definition.getPageNumber() - 1).getMediaBox().getWidth());
                if (extracted.labelFound) matchedLabels++;
                fields.add(buildField(task, definition, extracted.primaryValue,
                        extracted.secondaryValue, extracted.tertiaryValue,
                        extracted.labelFound ? new BigDecimal("98.0000") : BigDecimal.ZERO));
            }
        }
        if (matchedLabels < 20) {
            throw new IllegalArgumentException("PDF 未检测到可用的标准财务报表文字层");
        }
        log.info("PDF 本地解析完成: taskId={}, matchedLabels={}/{}", task.getId(), matchedLabels, fields.size());
        return fields;
    }

    private ExtractedValue extractValue(List<PositionedLine> lines,
                                        FinancialReportFieldTemplates.FieldDefinition definition,
                                        float pageWidth) {
        for (String alias : definition.getAliases()) {
            String compactAlias = compact(alias);
            for (PositionedLine line : lines) {
                if (!line.compactText.contains(compactAlias)) continue;
                PositionedToken rowToken = line.tokens.stream()
                        .filter(token -> String.valueOf(definition.getRowNumber()).equals(token.text))
                        .findFirst().orElse(null);
                if (rowToken == null) continue;

                float endX = Float.MAX_VALUE;
                for (PositionedToken token : line.tokens) {
                    if (token.x0 > rowToken.x1 && token.text.matches("\\d{1,3}")) {
                        endX = token.x0;
                        break;
                    }
                }
                String[] values = new String[]{"", "", ""};
                for (PositionedToken token : line.tokens) {
                    if (token.x0 <= rowToken.x1 || token.x0 >= endX || !MONEY.matcher(token.text).matches()) {
                        continue;
                    }
                    int column = nearestAmountColumn(definition.getFieldType(), rowToken.center(),
                            token.center(), pageWidth);
                    if (column >= 0 && column < values.length) {
                        values[column] = normalizeMoney(token.text);
                    }
                }
                return new ExtractedValue(true, values[0], values[1], values[2]);
            }
        }
        return new ExtractedValue(false, "", "", "");
    }

    private int nearestAmountColumn(String fieldType, float rowCenter, float moneyCenter, float pageWidth) {
        double[] ratios;
        if (FinancialReportFieldTemplates.BALANCE_SHEET.equals(fieldType)) {
            ratios = rowCenter < pageWidth / 2
                    ? new double[]{0.334, 0.450}
                    : new double[]{0.793, 0.910};
        } else if (FinancialReportFieldTemplates.INCOME_STATEMENT.equals(fieldType)) {
            ratios = new double[]{0.552, 0.727, 0.917};
        } else {
            ratios = new double[]{0.566, 0.735, 0.910};
        }
        int nearest = -1;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < ratios.length; i++) {
            double current = Math.abs(moneyCenter - pageWidth * ratios[i]);
            if (current < distance) {
                distance = current;
                nearest = i;
            }
        }
        return distance <= pageWidth * 0.09 ? nearest : -1;
    }

    private String compact(String text) {
        return text == null ? "" : text.replaceAll("[\\s:：,，、()（）\"'“”‘’－-]", "");
    }

    private String normalizeMoney(String value) {
        return value.replace("−", "-").replace("—", "-")
                .replace(",", "").replace("，", "").replaceAll("\\s", "");
    }

    private OcrFieldResult buildField(OcrTask task, FinancialReportFieldTemplates.FieldDefinition definition,
                                      String value, String secondaryValue, String tertiaryValue,
                                      BigDecimal confidence) {
        OcrFieldResult field = new OcrFieldResult();
        field.setOcrTask(task);
        field.setFieldCode(definition.getCode());
        field.setFieldName(definition.getName());
        field.setFieldValue(value);
        field.setSecondaryValue(secondaryValue);
        field.setTertiaryValue(tertiaryValue);
        field.setConfidenceScore(confidence);
        field.setConfidenceLevel(confidence.compareTo(new BigDecimal("90")) >= 0 ? "HIGH" : "LOW");
        field.setFieldType(definition.getFieldType());
        field.setPageNumber(definition.getPageNumber());
        field.setIsReviewed(0);
        field.setCreatedTime(LocalDateTime.now());
        field.setUpdatedTime(LocalDateTime.now());
        field.setDeleted(0);
        return field;
    }

    private void extractMetadata(OcrTask task, String text) {
        Matcher header = HEADER.matcher(text);
        if (header.find()) {
            task.setSourceEnterpriseName(header.group(1).replaceAll("\\s", "").trim());
            String rawPeriod = header.group(2).replace('年', '-').replace('月', '-').replace('/', '-').replace('.', '-');
            if (rawPeriod.endsWith("-")) rawPeriod = rawPeriod.substring(0, rawPeriod.length() - 1);
            String[] parts = rawPeriod.split("-");
            if (parts.length >= 2) {
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                task.setSourceReportPeriod(String.format("%04d-%02d", year, month));
                task.setSourceReportDate(parts.length >= 3
                        ? LocalDate.of(year, month, Integer.parseInt(parts[2]))
                        : YearMonth.of(year, month).atEndOfMonth());
            }
        }
        Matcher unit = UNIT.matcher(text);
        if (unit.find()) {
            task.setSourceUnit(unit.group(1).contains("万") ? "万元" : "元");
        }
    }

    private static final class ExtractedValue {
        private final boolean labelFound;
        private final String primaryValue;
        private final String secondaryValue;
        private final String tertiaryValue;

        private ExtractedValue(boolean labelFound, String primaryValue,
                               String secondaryValue, String tertiaryValue) {
            this.labelFound = labelFound;
            this.primaryValue = primaryValue;
            this.secondaryValue = secondaryValue;
            this.tertiaryValue = tertiaryValue;
        }
    }

    private static final class PositionedTextStripper extends PDFTextStripper {
        private final List<PositionedGlyph> glyphs = new ArrayList<>();

        private PositionedTextStripper() throws IOException {
            super();
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            for (TextPosition position : textPositions) {
                String unicode = position.getUnicode();
                if (unicode != null && !unicode.trim().isEmpty()) {
                    glyphs.add(new PositionedGlyph(unicode.trim(), position.getXDirAdj(),
                            position.getYDirAdj(), position.getWidthDirAdj()));
                }
            }
            super.writeString(text, textPositions);
        }

        private List<PositionedLine> lines() {
            List<PositionedGlyph> sorted = new ArrayList<>(glyphs);
            sorted.sort(Comparator.comparingDouble((PositionedGlyph glyph) -> glyph.y)
                    .thenComparingDouble(glyph -> glyph.x0));
            List<List<PositionedGlyph>> groups = new ArrayList<>();
            List<Float> groupY = new ArrayList<>();
            for (PositionedGlyph glyph : sorted) {
                int index = -1;
                for (int i = 0; i < groupY.size(); i++) {
                    if (Math.abs(groupY.get(i) - glyph.y) <= 2.5f) {
                        index = i;
                        break;
                    }
                }
                if (index < 0) {
                    groups.add(new ArrayList<>());
                    groupY.add(glyph.y);
                    index = groups.size() - 1;
                }
                groups.get(index).add(glyph);
            }
            List<PositionedLine> result = new ArrayList<>();
            for (List<PositionedGlyph> group : groups) {
                group.sort(Comparator.comparingDouble(glyph -> glyph.x0));
                result.add(PositionedLine.of(group));
            }
            return result;
        }
    }

    private static final class PositionedLine {
        private final String compactText;
        private final List<PositionedToken> tokens;

        private PositionedLine(String compactText, List<PositionedToken> tokens) {
            this.compactText = compactText;
            this.tokens = tokens;
        }

        private static PositionedLine of(List<PositionedGlyph> glyphs) {
            StringBuilder compact = new StringBuilder();
            List<PositionedToken> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            float tokenX0 = 0;
            float tokenX1 = 0;
            for (PositionedGlyph glyph : glyphs) {
                compact.append(glyph.text);
                if (current.length() == 0 || glyph.x0 - tokenX1 <= 2.2f) {
                    if (current.length() == 0) tokenX0 = glyph.x0;
                    current.append(glyph.text);
                    tokenX1 = glyph.x1();
                } else {
                    tokens.add(new PositionedToken(current.toString(), tokenX0, tokenX1));
                    current.setLength(0);
                    current.append(glyph.text);
                    tokenX0 = glyph.x0;
                    tokenX1 = glyph.x1();
                }
            }
            if (current.length() > 0) {
                tokens.add(new PositionedToken(current.toString(), tokenX0, tokenX1));
            }
            return new PositionedLine(compact.toString().replaceAll("[\\s:：,，、()（）\"'“”‘’－-]", ""), tokens);
        }
    }

    private static final class PositionedGlyph {
        private final String text;
        private final float x0;
        private final float y;
        private final float width;

        private PositionedGlyph(String text, float x0, float y, float width) {
            this.text = text;
            this.x0 = x0;
            this.y = y;
            this.width = width;
        }

        private float x1() {
            return x0 + width;
        }
    }

    private static final class PositionedToken {
        private final String text;
        private final float x0;
        private final float x1;

        private PositionedToken(String text, float x0, float x1) {
            this.text = text;
            this.x0 = x0;
            this.x1 = x1;
        }

        private float center() {
            return (x0 + x1) / 2;
        }
    }
}
