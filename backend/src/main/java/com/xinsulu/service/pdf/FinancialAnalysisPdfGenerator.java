package com.xinsulu.service.pdf;

import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.vo.AnalysisReportVO;
import com.xinsulu.vo.HealthScoreVO;
import com.xinsulu.vo.ReportDetailVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 生成可直接交付的中文财务分析 PDF。 */
@Component
public class FinancialAnalysisPdfGenerator {

    private static final Color NAVY = new Color(18, 48, 68);
    private static final Color TEAL = new Color(14, 143, 120);
    private static final Color MINT = new Color(226, 246, 240);
    private static final Color INK = new Color(24, 43, 54);
    private static final Color MUTED = new Color(103, 123, 136);
    private static final Color LINE = new Color(222, 231, 236);
    private static final Color PAPER = new Color(247, 250, 251);
    private static final Color AMBER = new Color(235, 164, 55);
    private static final Color RED = new Color(211, 76, 86);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] generate(AnalysisReportVO report, ReportDetailVO detail,
                           HealthScoreVO health, Map<String, Object> indicators) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDFont regular = loadFont(document, false);
            PDFont bold = loadFont(document, true);
            Composer composer = new Composer(document, regular, bold,
                    report.getEnterpriseName(), detail.getReportPeriod());

            composer.drawCover(report, detail, health);
            composer.startContentPage("01  /  经营摘要与评分画像");
            composer.drawLead(report.getExecutiveSummary());
            composer.drawScoreGrid(health);
            composer.drawIndicatorTable(indicators);

            composer.startContentPage("02  /  风险研判与行动建议");
            composer.drawSection("总体评价", report.getOverallAssessment(), TEAL);
            composer.drawSection("关键发现", report.getKeyFindings(), NAVY);
            composer.drawSection("主要风险", report.getRiskAnalysis(), RED);
            composer.drawSection("积极因素", report.getPositiveFactors(), TEAL);
            composer.drawSection("改进建议", report.getImprovementSuggestions(), AMBER);
            composer.drawSection("数据质量说明", report.getDataQualityNotes(), MUTED);
            composer.finish();

            PDDocumentInformation metadata = document.getDocumentInformation();
            metadata.setTitle(safe(report.getReportTitle(), "财务分析报告"));
            metadata.setAuthor("鑫速录财务智能分析系统");
            metadata.setSubject("企业财务健康度与风险分析");
            metadata.setCreator("鑫速录 XINSULU");
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            throw new BusinessException("PDF报告生成失败：" + exception.getMessage(), exception);
        }
    }

    private PDFont loadFont(PDDocument document, boolean bold) throws IOException {
        String configured = System.getenv(bold ? "PDF_BOLD_FONT_PATH" : "PDF_FONT_PATH");
        List<String> candidates = new ArrayList<>();
        if (configured != null && !configured.trim().isEmpty()) {
            candidates.add(configured.trim());
        }
        candidates.addAll(bold
                ? Arrays.asList("C:/Windows/Fonts/Dengb.ttf", "C:/Windows/Fonts/simhei.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttf")
                : Arrays.asList("C:/Windows/Fonts/Deng.ttf", "C:/Windows/Fonts/NotoSansSC-VF.ttf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttf"));
        for (String path : candidates) {
            File file = new File(path);
            if (file.isFile()) {
                try (FileInputStream input = new FileInputStream(file)) {
                    return PDType0Font.load(document, input, true);
                }
            }
        }
        throw new IOException("未找到中文字体，请配置 PDF_FONT_PATH 和 PDF_BOLD_FONT_PATH");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static final class Composer {
        private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
        private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
        private static final float MARGIN = 48;
        private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2;

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private final String enterpriseName;
        private final String reportPeriod;
        private PDPage page;
        private PDPageContentStream stream;
        private float y;

        private Composer(PDDocument document, PDFont regular, PDFont bold,
                         String enterpriseName, String reportPeriod) {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            this.enterpriseName = safe(enterpriseName, "企业");
            this.reportPeriod = safe(reportPeriod, "—");
        }

        private void drawCover(AnalysisReportVO report, ReportDetailVO detail, HealthScoreVO health)
                throws IOException {
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, PAPER);
            fillRect(0, PAGE_HEIGHT - 292, PAGE_WIDTH, 292, NAVY);
            fillRect(MARGIN, PAGE_HEIGHT - 64, 42, 5, TEAL);
            text("鑫速录  XINSULU", MARGIN, PAGE_HEIGHT - 92, 13, bold, Color.WHITE);
            text("FINANCIAL INTELLIGENCE", MARGIN, PAGE_HEIGHT - 111, 8.5f, regular,
                    new Color(170, 202, 214));
            text("企业财务智能分析报告", MARGIN, PAGE_HEIGHT - 174, 27, bold, Color.WHITE);
            text("FINANCIAL ANALYSIS REPORT", MARGIN, PAGE_HEIGHT - 200, 10, regular,
                    new Color(170, 202, 214));
            drawWrapped(enterpriseName, MARGIN, PAGE_HEIGHT - 244, CONTENT_WIDTH - 120,
                    15, 23, bold, Color.WHITE, 2);

            y = PAGE_HEIGHT - 346;
            text("报告信息", MARGIN, y, 12, bold, TEAL);
            y -= 30;
            drawMetaRow("报表期间", reportPeriod, "生成时间", LocalDateTime.now().format(TIME_FORMAT));
            drawMetaRow("报告类型", "综合财务健康分析", "分析方法", "规则引擎 · 自动测算");
            drawMetaRow("归档编号", "REPORT-" + String.format("%06d", detail.getArchiveId()),
                    "报告版本", "V" + (report.getVersion() == null ? 1 : report.getVersion()));

            y -= 20;
            fillRect(MARGIN, y - 104, CONTENT_WIDTH, 104, Color.WHITE);
            fillRect(MARGIN, y - 104, 7, 104, TEAL);
            text("财务健康度", MARGIN + 26, y - 30, 11, regular, MUTED);
            text(score(health == null ? null : health.getTotalScore()), MARGIN + 26, y - 73,
                    31, bold, scoreColor(health));
            text("/ 100", MARGIN + 103, y - 70, 11, regular, MUTED);
            text("风险等级", MARGIN + 300, y - 30, 11, regular, MUTED);
            text(riskLabel(health == null ? null : health.getRiskLevel()), MARGIN + 300, y - 66,
                    21, bold, scoreColor(health));

            text("本报告由鑫速录财务智能分析系统基于已归档财务报表自动生成。",
                    MARGIN, 66, 9, regular, MUTED);
            text("数据驱动 · 风险可见 · 决策有据", MARGIN, 48, 9, bold, TEAL);
            closeStream();
        }

        private void startContentPage(String chapter) throws IOException {
            closeStream();
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT, PAPER);
            fillRect(0, PAGE_HEIGHT - 58, PAGE_WIDTH, 58, NAVY);
            text("鑫速录", MARGIN, PAGE_HEIGHT - 36, 12, bold, Color.WHITE);
            text(enterpriseName + "  ·  " + reportPeriod, MARGIN + 72, PAGE_HEIGHT - 36,
                    9, regular, new Color(190, 211, 220));
            text(chapter, MARGIN, PAGE_HEIGHT - 94, 12, bold, NAVY);
            fillRect(MARGIN, PAGE_HEIGHT - 105, 36, 3, TEAL);
            y = PAGE_HEIGHT - 128;
        }

        private void drawLead(String content) throws IOException {
            List<String> lines = wrap(cleanSection(content), regular, 11, CONTENT_WIDTH - 34);
            float height = Math.max(92, lines.size() * 18 + 42);
            ensure(height + 16);
            fillRect(MARGIN, y - height, CONTENT_WIDTH, height, Color.WHITE);
            fillRect(MARGIN, y - height, 6, height, TEAL);
            text("执行摘要", MARGIN + 22, y - 28, 13, bold, NAVY);
            float lineY = y - 54;
            for (String line : lines) {
                text(line, MARGIN + 22, lineY, 11, regular, INK);
                lineY -= 18;
            }
            y -= height + 20;
        }

        private void drawScoreGrid(HealthScoreVO health) throws IOException {
            ensure(150);
            text("五维评分", MARGIN, y, 13, bold, NAVY);
            y -= 18;
            String[] labels = {"偿债能力", "盈利能力", "现金流质量", "运营效率", "成长能力"};
            BigDecimal[] values = health == null ? new BigDecimal[5] : new BigDecimal[]{
                    health.getSolvencyScore(), health.getProfitabilityScore(), health.getCashFlowScore(),
                    health.getOperationScore(), health.getGrowthScore()
            };
            float gap = 8;
            float width = (CONTENT_WIDTH - gap * 4) / 5;
            for (int i = 0; i < labels.length; i++) {
                float x = MARGIN + i * (width + gap);
                fillRect(x, y - 86, width, 86, Color.WHITE);
                fillRect(x, y - 4, width, 4, i < 2 ? TEAL : NAVY);
                text(labels[i], x + 10, y - 28, 8.5f, regular, MUTED);
                text(score(values[i]), x + 10, y - 62, 19, bold,
                        values[i] != null && values[i].compareTo(new BigDecimal("60")) < 0 ? RED : TEAL);
            }
            y -= 112;
        }

        private void drawIndicatorTable(Map<String, Object> indicators) throws IOException {
            ensure(250);
            text("关键财务指标", MARGIN, y, 13, bold, NAVY);
            y -= 22;
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put("currentRatio", "流动比率");
            labels.put("quickRatio", "速动比率");
            labels.put("debtToAssetRatio", "资产负债率");
            labels.put("grossProfitMargin", "销售毛利率");
            labels.put("netProfitMargin", "销售净利率");
            labels.put("roe", "净资产收益率");
            labels.put("totalAssetTurnover", "总资产周转率");
            labels.put("operatingCashToRevenue", "经营现金/收入");
            int index = 0;
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                float x = MARGIN + (index % 2) * (CONTENT_WIDTH / 2);
                if (index % 2 == 0) {
                    ensure(34);
                    fillRect(MARGIN, y - 32, CONTENT_WIDTH, 32, index % 4 == 0 ? Color.WHITE : new Color(241, 246, 248));
                }
                text(entry.getValue(), x + 12, y - 21, 9.5f, regular, INK);
                text(formatIndicator(indicators == null ? null : indicators.get(entry.getKey())),
                        x + 176, y - 21, 9.5f, bold, TEAL);
                if (index % 2 == 1) y -= 32;
                index++;
            }
            if (index % 2 == 1) y -= 32;
            y -= 12;
        }

        private void drawSection(String title, String content, Color accent) throws IOException {
            String cleaned = cleanSection(content);
            if (cleaned.isEmpty()) return;
            List<String> lines = wrap(cleaned, regular, 10.5f, CONTENT_WIDTH - 36);
            ensure(62);
            text(title, MARGIN, y, 13, bold, accent);
            y -= 16;
            fillRect(MARGIN, y - 1, CONTENT_WIDTH, 1, LINE);
            y -= 18;
            for (String line : lines) {
                ensure(20);
                if (line.startsWith("- ")) {
                    fillRect(MARGIN + 2, y - 3, 5, 5, accent);
                    text(line.substring(2), MARGIN + 18, y, 10.5f, regular, INK);
                } else {
                    text(line, MARGIN + 2, y, 10.5f, regular, INK);
                }
                y -= 18;
            }
            y -= 20;
        }

        private void drawMetaRow(String label1, String value1, String label2, String value2) throws IOException {
            text(label1, MARGIN, y, 9.5f, regular, MUTED);
            text(value1, MARGIN + 78, y, 10.5f, bold, INK);
            text(label2, MARGIN + 285, y, 9.5f, regular, MUTED);
            text(value2, MARGIN + 353, y, 10.5f, bold, INK);
            y -= 36;
        }

        private void ensure(float height) throws IOException {
            if (y - height < 52) {
                startContentPage("续  /  分析正文");
            }
        }

        private void finish() throws IOException {
            closeStream();
            int count = document.getNumberOfPages();
            for (int i = 0; i < count; i++) {
                PDPage footerPage = document.getPage(i);
                try (PDPageContentStream footer = new PDPageContentStream(document, footerPage,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {
                    footer.setStrokingColor(LINE);
                    footer.moveTo(MARGIN, 35);
                    footer.lineTo(PAGE_WIDTH - MARGIN, 35);
                    footer.stroke();
                    drawText(footer, "鑫速录 · 企业财务智能分析", MARGIN, 20, 7.5f, regular, MUTED);
                    drawText(footer, String.format("%02d / %02d", i + 1, count), PAGE_WIDTH - 90, 20,
                            7.5f, regular, MUTED);
                }
            }
        }

        private void closeStream() throws IOException {
            if (stream != null) {
                stream.close();
                stream = null;
            }
        }

        private void fillRect(float x, float topY, float width, float height, Color color) throws IOException {
            stream.setNonStrokingColor(color);
            stream.addRect(x, topY, width, height);
            stream.fill();
        }

        private void text(String value, float x, float y, float size, PDFont font, Color color) throws IOException {
            drawText(stream, safe(value, "—"), x, y, size, font, color);
        }

        private void drawWrapped(String value, float x, float startY, float width, float size,
                                 float leading, PDFont font, Color color, int maxLines) throws IOException {
            List<String> lines = wrap(value, font, size, width);
            float lineY = startY;
            for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
                text(lines.get(i), x, lineY, size, font, color);
                lineY -= leading;
            }
        }

        private static void drawText(PDPageContentStream target, String value, float x, float y,
                                     float size, PDFont font, Color color) throws IOException {
            target.beginText();
            target.setFont(font, size);
            target.setNonStrokingColor(color);
            target.newLineAtOffset(x, y);
            target.showText(value);
            target.endText();
        }

        private static List<String> wrap(String text, PDFont font, float size, float width) throws IOException {
            List<String> lines = new ArrayList<>();
            String normalized = safe(text, "暂无可用分析结论").replace("\r", "");
            for (String paragraph : normalized.split("\n")) {
                String value = paragraph.trim();
                if (value.isEmpty()) continue;
                StringBuilder line = new StringBuilder();
                for (int offset = 0; offset < value.length();) {
                    int codePoint = value.codePointAt(offset);
                    String token = new String(Character.toChars(codePoint));
                    String candidate = line.toString() + token;
                    float candidateWidth = font.getStringWidth(candidate) / 1000f * size;
                    boolean closingPunctuation = "，。！？；：、）》】”’".contains(token);
                    if (candidateWidth > width && line.length() > 0 && !closingPunctuation) {
                        lines.add(line.toString());
                        line.setLength(0);
                    }
                    line.append(token);
                    offset += Character.charCount(codePoint);
                }
                if (line.length() > 0) lines.add(line.toString());
            }
            return lines;
        }

        private static String cleanSection(String value) {
            if (value == null) return "";
            return value.trim()
                    .replaceFirst("^[一二三四五六七八九十]+、[^\\n]*[\\n\\r]*", "")
                    .replaceAll("(?m)^\\s*[•·]\\s*", "- ")
                    .replaceAll("(?m)^\\s*(\\d+)[.、]\\s*", "- ");
        }

        private static String score(BigDecimal value) {
            return value == null ? "—" : value.setScale(0, BigDecimal.ROUND_HALF_UP).toPlainString();
        }

        private static Color scoreColor(HealthScoreVO health) {
            if (health == null || health.getTotalScore() == null) return MUTED;
            if (health.getTotalScore().compareTo(new BigDecimal("40")) < 0) return RED;
            if (health.getTotalScore().compareTo(new BigDecimal("70")) < 0) return AMBER;
            return TEAL;
        }

        private static String riskLabel(String level) {
            if (level == null) return "待评估";
            Map<String, String> labels = new LinkedHashMap<>();
            labels.put("HEALTHY", "健康");
            labels.put("NORMAL", "基本健康");
            labels.put("ATTENTION", "需关注");
            labels.put("WARNING", "需关注");
            labels.put("DANGEROUS", "高风险");
            labels.put("CRITICAL", "严重风险");
            return labels.containsKey(level) ? labels.get(level) : level;
        }

        private static String formatIndicator(Object value) {
            if (value == null) return "—";
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString();
            }
            if (value instanceof Number) {
                return new BigDecimal(value.toString()).setScale(2, BigDecimal.ROUND_HALF_UP)
                        .stripTrailingZeros().toPlainString();
            }
            return value.toString();
        }
    }
}
