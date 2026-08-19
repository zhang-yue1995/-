package com.xinsulu;

import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.service.ocr.FinancialReportPdfExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinancialReportPdfExtractorTest {

    @Test
    void extractsTheUploadedThreeStatementPdf() throws Exception {
        File file = new File("uploads/c03ba4ac-7a2e-4e74-8fa4-8ea29b84933d.pdf");
        Assumptions.assumeTrue(file.isFile(), "本地真实报表样本不存在时跳过");
        OcrTask task = new OcrTask();
        task.setId(999L);
        List<OcrFieldResult> fields = new FinancialReportPdfExtractor().extract(task, file);

        assertEquals(140, fields.size());
        assertEquals("江苏曼斯特机电科技有限公司", task.getSourceEnterpriseName());
        assertEquals("2026-03", task.getSourceReportPeriod());
        assertEquals("2026-03-31", task.getSourceReportDate().toString());
        assertEquals("1034959.57", value(fields, "BS.001"));
        assertEquals("1425966.10", secondaryValue(fields, "BS.001"));
        assertEquals("773769.57", value(fields, "IS.001"));
        assertEquals("2351280.19", secondaryValue(fields, "IS.001"));
        assertEquals("20826.53", tertiaryValue(fields, "IS.001"));
        assertEquals("", value(fields, "CF.030"));
        assertEquals("514.25", secondaryValue(fields, "CF.030"));
        assertEquals("-391006.53", value(fields, "CF.036"));
        assertEquals("744734.09", secondaryValue(fields, "CF.036"));
        assertEquals("-93588.34", tertiaryValue(fields, "CF.036"));
    }

    private String value(List<OcrFieldResult> fields, String code) {
        return fields.stream().filter(field -> code.equals(field.getFieldCode()))
                .findFirst().orElseThrow(AssertionError::new).getFieldValue();
    }

    private String secondaryValue(List<OcrFieldResult> fields, String code) {
        return fields.stream().filter(field -> code.equals(field.getFieldCode()))
                .findFirst().orElseThrow(AssertionError::new).getSecondaryValue();
    }

    private String tertiaryValue(List<OcrFieldResult> fields, String code) {
        return fields.stream().filter(field -> code.equals(field.getFieldCode()))
                .findFirst().orElseThrow(AssertionError::new).getTertiaryValue();
    }
}
