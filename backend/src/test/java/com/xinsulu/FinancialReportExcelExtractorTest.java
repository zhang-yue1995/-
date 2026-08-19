package com.xinsulu;

import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.service.ocr.FinancialReportExcelExtractor;
import com.xinsulu.service.ocr.FinancialReportFieldNormalizer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialReportExcelExtractorTest {

    @Test
    void adaptsTheTwoProvidedPartialStatementWorkbooks() throws Exception {
        assertWorkbook("2024财务报表-思创.xlsx", "2024-12",
                "28041.5", "17670.64", "1284433.59", "207477.87");
        assertWorkbook("2025.1-3财务报表-思创.xlsx", "2025-03",
                "45091.62", "28041.5", "87575.22", "87575.22");
    }

    private void assertWorkbook(String filename, String period,
                                String cashEnding, String cashBeginning,
                                String revenueYearToDate, String revenueMonth) throws Exception {
        File file = Paths.get("..", "..", "报表原件及管理后台系统原页", "测试原件", filename)
                .normalize().toFile();
        Assumptions.assumeTrue(file.isFile(), "本地真实 Excel 报表样本不存在时跳过");

        OcrTask task = new OcrTask();
        task.setId(1000L);
        FinancialReportExcelExtractor extractor = new FinancialReportExcelExtractor();
        List<OcrFieldResult> raw = extractor.extract(task, file);
        List<OcrFieldResult> fields = new FinancialReportFieldNormalizer().normalize(task, raw);

        assertEquals(140, fields.size());
        assertEquals("南京蔚来思创科技有限公司", task.getSourceEnterpriseName());
        assertEquals(period, task.getSourceReportPeriod());
        assertEquals("元", task.getSourceUnit());
        assertEquals(cashEnding, value(fields, "BS.001"));
        assertEquals(cashBeginning, secondaryValue(fields, "BS.001"));
        assertEquals(revenueYearToDate, value(fields, "IS.001"));
        assertEquals("", secondaryValue(fields, "IS.001"));
        assertEquals(revenueMonth, tertiaryValue(fields, "IS.001"));

        // 两份样本都没有现金流量表：字段行必须完整保留，所有金额留空。
        for (OcrFieldResult field : fields) {
            if ("CASH_FLOW_STATEMENT".equals(field.getFieldType())) {
                assertEquals("", field.getFieldValue());
                assertEquals("", field.getSecondaryValue());
                assertEquals("", field.getTertiaryValue());
            }
        }
    }

    private String value(List<OcrFieldResult> fields, String code) {
        return find(fields, code).getFieldValue();
    }

    private String secondaryValue(List<OcrFieldResult> fields, String code) {
        return find(fields, code).getSecondaryValue();
    }

    private String tertiaryValue(List<OcrFieldResult> fields, String code) {
        return find(fields, code).getTertiaryValue();
    }

    private OcrFieldResult find(List<OcrFieldResult> fields, String code) {
        return fields.stream().filter(field -> code.equals(field.getFieldCode()))
                .findFirst().orElseThrow(AssertionError::new);
    }
}
