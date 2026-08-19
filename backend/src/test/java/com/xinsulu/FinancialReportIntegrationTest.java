package com.xinsulu;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xinsulu.config.DemoDataInitializer;
import com.xinsulu.entity.FinancialReportArchive;
import com.xinsulu.repository.EnterpriseRepository;
import com.xinsulu.repository.FinancialReportArchiveRepository;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.repository.UploadedFileRepository;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.AnalysisReportVO;
import com.xinsulu.vo.HealthScoreVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:xinsulu-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "xinsulu.demo-data.enabled=true",
        "xinsulu.storage.upload-dir=target/test-uploads"
})
class FinancialReportIntegrationTest {

    @Autowired
    private FinancialReportArchiveRepository archiveRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private DemoDataInitializer demoDataInitializer;

    @Autowired
    private FinancialReportService financialReportService;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void referenceReportMatchesSourceDocuments() {
        FinancialReportArchive archive = findReferenceArchive();

        assertDecimal("7923544.71", archive.getTotalAssets());
        assertDecimal("7995201.81", archive.getTotalLiabilities());
        assertDecimal("-71657.10", archive.getTotalEquity());
        assertDecimal("773769.57", archive.getRevenue());
        assertDecimal("-9950.59", archive.getNetProfit());
        assertDecimal("-391006.53", archive.getOperatingCashFlow());

        Map<?, ?> statements = (Map<?, ?>) financialReportService.getStatements(archive.getId());
        assertEquals(65, statementItems(statements, "balanceSheet").size());
        assertEquals(37, statementItems(statements, "incomeStatement").size());
        assertEquals(38, statementItems(statements, "cashFlowStatement").size());
    }

    @Test
    void workbookModelProducesExpectedIndicatorsAndHealthScore() {
        Long reportId = findReferenceArchive().getId();
        Map<String, Object> indicators = financialReportService.calculateIndicators(reportId);

        assertDecimal("0.9336", (BigDecimal) indicators.get("currentRatio"));
        assertDecimal("100.9044", (BigDecimal) indicators.get("debtToAssetRatio"));
        assertDecimal("42.1732", (BigDecimal) indicators.get("grossProfitMargin"));
        assertDecimal("-0.5053", (BigDecimal) indicators.get("operatingCashToRevenue"));
        assertNull(indicators.get("roe"), "负权益时ROE应标记为不可计算");

        HealthScoreVO score = financialReportService.calculateHealthScore(reportId);
        assertDecimal("22", score.getTotalScore());
        assertDecimal("29.50", score.getSolvencyScore());
        assertDecimal("31.75", score.getProfitabilityScore());
        assertDecimal("9.00", score.getOperationScore());
        assertDecimal("9.00", score.getCashFlowScore());
        assertDecimal("22.67", score.getGrowthScore());
        assertEquals("DANGEROUS", score.getRiskLevel());
    }

    @Test
    void repeatedCalculationsAndAnalysisGenerationAreIdempotent() {
        Long reportId = findReferenceArchive().getId();

        financialReportService.calculateIndicators(reportId);
        financialReportService.calculateIndicators(reportId);
        financialReportService.calculateHealthScore(reportId);
        HealthScoreVO secondScore = financialReportService.calculateHealthScore(reportId);
        assertDecimal("22", secondScore.getTotalScore());

        AnalysisReportVO first = financialReportService.generateAnalysisReport(reportId);
        AnalysisReportVO second = financialReportService.generateAnalysisReport(reportId);
        assertNotNull(first.getReportId());
        assertEquals(first.getReportId(), second.getReportId());
        assertTrue(second.getRiskAnalysis().contains("现金流风险"));
    }

    @Test
    void managementApiFlowReturnsDeployableDataContract() throws Exception {
        Long reportId = findReferenceArchive().getId();

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
        String authorization = loginAsAdmin();

        mockMvc.perform(get("/api/dashboard/stats").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalEnterprises").value(3))
                .andExpect(jsonPath("$.data.totalReports").value(3))
                .andExpect(jsonPath("$.data.highRiskEnterprises").value(1));

        mockMvc.perform(get("/api/enterprises")
                        .header("Authorization", authorization)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "曼斯特"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].latestHealthScore").value(22));

        mockMvc.perform(get("/api/reports/{id}/statements", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceSheet.items.length()").value(65))
                .andExpect(jsonPath("$.data.incomeStatement.items.length()").value(37))
                .andExpect(jsonPath("$.data.cashFlowStatement.items.length()").value(38));
    }

    @Test
    @Transactional
    void analysisPdfAndApprovalFlowAreRealAndStateful() throws Exception {
        FinancialReportArchive archive = findReferenceArchive();
        Long reportId = archive.getId();
        String authorization = loginAsAdmin();

        mockMvc.perform(post("/api/analysis-reports/generate/{id}", reportId)
                        .header("Authorization", authorization)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("generated"))
                .andExpect(jsonPath("$.data.reportPeriod").value("2026-03"));

        mockMvc.perform(post("/api/analysis-reports/{id}/submit-approval", reportId)
                        .header("Authorization", authorization)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"))
                .andExpect(jsonPath("$.data.submittedBy").value("admin"));

        mockMvc.perform(get("/api/analysis-reports/report/{id}", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("pending_approval"));

        mockMvc.perform(get("/api/reports/{id}", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus").value("pending_approval"))
                .andExpect(jsonPath("$.data.approvalSubmittedBy").value("admin"));

        mockMvc.perform(put("/api/reports/{id}/complete-approval", reportId)
                        .header("Authorization", authorization)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.approvedBy").value("admin"));

        MvcResult pdfResult = mockMvc.perform(get("/api/analysis-reports/export/{id}", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andReturn();
        byte[] pdf = pdfResult.getResponse().getContentAsByteArray();
        assertTrue(pdf.length > 10000, "PDF应包含完整排版内容与嵌入字体");
        assertEquals("%PDF-", new String(pdf, 0, 5, "US-ASCII"));
        String disposition = pdfResult.getResponse().getHeader("Content-Disposition");
        assertNotNull(disposition);
        assertTrue(disposition.contains("_2026-03_"));
        assertTrue(disposition.endsWith(".pdf"));

        Path outputDir = Paths.get("..", "output", "pdf");
        Files.createDirectories(outputDir);
        String filename = archive.getEnterprise().getEnterpriseName()
                + "_" + archive.getReportPeriod() + "_报表分析.pdf";
        Files.write(outputDir.resolve(filename), pdf);
    }

    @Test
    @Transactional
    void uploadOcrArchiveAndReviewFlowPersistsStatements() throws Exception {
        String authorization = loginAsAdmin();
        byte[] fileContent = "%PDF-1.4\nxinsulu integration test".getBytes("UTF-8");
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", fileContent);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn();
        long fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertEquals("admin", uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new AssertionError("上传记录不存在"))
                .getUploadedBy().getUsername());

        mockMvc.perform(get("/api/files/{id}/content", fileId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(content().bytes(fileContent));

        MvcResult ocrResult = mockMvc.perform(post("/api/ocr/recognize")
                        .param("fileId", String.valueOf(fileId))
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"))
                .andReturn();
        long taskId = objectMapper.readTree(ocrResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        assertEquals("admin", ocrTaskRepository.findById(taskId)
                .orElseThrow(() -> new AssertionError("OCR任务不存在"))
                .getCreatedBy().getUsername());

        ObjectNode archiveRequest = objectMapper.createObjectNode();
        archiveRequest.put("enterpriseId", findReferenceArchive().getEnterprise().getId());
        archiveRequest.put("reportPeriod", "2026-04");
        archiveRequest.put("reportDate", "2026-04-30");
        archiveRequest.put("reportType", "MONTHLY");
        archiveRequest.put("year", 2026);
        archiveRequest.put("month", 4);
        archiveRequest.put("dataSource", "OCR_AUTO");
        archiveRequest.put("filingStatus", "DRAFT");
        archiveRequest.put("managerName", "张经理");
        archiveRequest.put("fileId", fileId);
        archiveRequest.put("ocrTaskId", taskId);
        MvcResult archiveResult = mockMvc.perform(post("/api/reports/archive")
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(archiveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();
        long reportId = objectMapper.readTree(archiveResult.getResponse().getContentAsString())
                .path("data").asLong();

        mockMvc.perform(get("/api/enterprises")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "曼斯特")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].latestReportPeriod").value("2026-04"))
                .andExpect(jsonPath("$.data.list[0].managerName").value("张经理"));

        // 管理端企业与报表列表必须按归档期逐条返回，不能只保留企业最新一期。
        mockMvc.perform(get("/api/reports")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("keyword", "曼斯特")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].reportPeriod").value("2026-04"))
                .andExpect(jsonPath("$.data.list[1].reportPeriod").value("2026-03"))
                .andExpect(jsonPath("$.data.list[0].enterpriseCreditCode").isNotEmpty());

        MvcResult fieldsResult = mockMvc.perform(get("/api/ocr/tasks/{id}/results", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode fieldResults = objectMapper.readTree(fieldsResult.getResponse().getContentAsString())
                .path("data").path("fieldResults");
        assertEquals(140, fieldResults.size());
        assertEquals("张经理", archiveRepository.findById(reportId)
                .orElseThrow(() -> new AssertionError("归档不存在")).getManagerName());
        ArrayNode reviews = objectMapper.createArrayNode();
        fieldResults.forEach(fieldNode -> {
            ObjectNode review = reviews.addObject();
            review.put("fieldResultId", fieldNode.path("id").asLong());
            review.put("originalValue", fieldNode.path("fieldValue").asText());
            review.put("correctedValue", fieldNode.path("fieldValue").asText());
            review.put("correctedSecondaryValue", fieldNode.path("secondaryValue").asText());
            review.put("correctedTertiaryValue", fieldNode.path("tertiaryValue").asText());
            if ("BS.001".equals(fieldNode.path("fieldCode").asText())) {
                review.put("correctedSecondaryValue", "1425966.10");
            } else if ("IS.001".equals(fieldNode.path("fieldCode").asText())) {
                review.put("correctedSecondaryValue", "2351280.19");
                review.put("correctedTertiaryValue", "20826.53");
            } else if ("CF.030".equals(fieldNode.path("fieldCode").asText())) {
                review.put("correctedSecondaryValue", "514.25");
            }
            review.put("isConfirmedCorrect", true);
            review.put("confidence", fieldNode.path("confidenceScore").asDouble());
        });

        mockMvc.perform(put("/api/reports/{id}/review", reportId)
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(reviews)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/reports/{id}/statements", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceSheet.items.length()").value(65))
                .andExpect(jsonPath("$.data.incomeStatement.items.length()").value(37))
                .andExpect(jsonPath("$.data.cashFlowStatement.items.length()").value(38))
                .andExpect(jsonPath("$.data.balanceSheet.items[0].beginningBalance").value(1425966.10))
                .andExpect(jsonPath("$.data.incomeStatement.items[0].previousPeriodAmount").value(2351280.19))
                .andExpect(jsonPath("$.data.incomeStatement.items[0].monthlyAmount").value(20826.53))
                .andExpect(jsonPath("$.data.cashFlowStatement.items[29].previousPeriodAmount").value(514.25));

        // 同一企业新增一期后，趋势接口必须即时返回按期间升序排列的两期数据。
        mockMvc.perform(get("/api/trends/enterprise/{id}", findReferenceArchive().getEnterprise().getId())
                        .header("Authorization", authorization)
                        .param("indicatorCode", "debtToAssetRatio")
                        .param("periods", "36"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dataList.length()").value(2))
                .andExpect(jsonPath("$.data.dataList[0].reportPeriod").value("2026-03"))
                .andExpect(jsonPath("$.data.dataList[1].reportPeriod").value("2026-04"));

        mockMvc.perform(get("/api/reports/{id}", reportId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.filingStatus").value("REVIEWED"));

        mockMvc.perform(get("/api/files")
                        .header("Authorization", authorization)
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("uploadDate", LocalDate.now().toString())
                        .param("enterpriseName", "曼斯特"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].fileId").value(fileId))
                .andExpect(jsonPath("$.data.list[0].archiveId").value(reportId))
                .andExpect(jsonPath("$.data.list[0].filingStatus").value("REVIEWED"));

        mockMvc.perform(delete("/api/files/{id}", fileId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/files")
                        .header("Authorization", authorization)
                        .param("enterpriseName", "曼斯特"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
        assertEquals(1, archiveRepository.findById(reportId)
                .orElseThrow(() -> new AssertionError("归档不存在")).getDeleted());
        assertEquals(1, uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new AssertionError("上传记录不存在")).getDeleted());
        assertEquals(1, ocrTaskRepository.findById(taskId)
                .orElseThrow(() -> new AssertionError("OCR任务不存在")).getDeleted());
    }

    @Test
    @Transactional
    void excelUploadAndRecognitionFlowAdaptsPartialWorkbook() throws Exception {
        Path sample = Paths.get("..", "..", "报表原件及管理后台系统原页", "测试原件",
                "2025.1-3财务报表-思创.xlsx").normalize();
        Assumptions.assumeTrue(Files.isRegularFile(sample), "本地真实 Excel 报表样本不存在时跳过");

        String authorization = loginAsAdmin();
        MockMultipartFile workbook = new MockMultipartFile(
                "file", sample.getFileName().toString(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                Files.readAllBytes(sample));

        MvcResult uploadResult = mockMvc.perform(multipart("/api/files/upload")
                        .file(workbook)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileType").value("XLSX"))
                .andReturn();
        long fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult recognizeResult = mockMvc.perform(post("/api/ocr/recognize")
                        .param("fileId", String.valueOf(fileId))
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.provider").value("EXCEL_WORKBOOK"))
                .andReturn();
        long taskId = objectMapper.readTree(recognizeResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        MvcResult result = mockMvc.perform(get("/api/ocr/tasks/{id}/results", taskId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseName").value("南京蔚来思创科技有限公司"))
                .andExpect(jsonPath("$.data.reportPeriod").value("2025-03"))
                .andExpect(jsonPath("$.data.unit").value("元"))
                .andExpect(jsonPath("$.data.fieldResults.length()").value(140))
                .andReturn();

        JsonNode fields = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("fieldResults");
        JsonNode cash = null;
        JsonNode revenue = null;
        int populatedCashFlowValues = 0;
        for (JsonNode field : fields) {
            if ("BS.001".equals(field.path("fieldCode").asText())) cash = field;
            if ("IS.001".equals(field.path("fieldCode").asText())) revenue = field;
            if ("CASH_FLOW_STATEMENT".equals(field.path("fieldType").asText())
                    && (!field.path("fieldValue").asText().isEmpty()
                    || !field.path("secondaryValue").asText().isEmpty()
                    || !field.path("tertiaryValue").asText().isEmpty())) {
                populatedCashFlowValues++;
            }
        }
        assertNotNull(cash);
        assertNotNull(revenue);
        assertEquals("45091.62", cash.path("fieldValue").asText());
        assertEquals("28041.5", cash.path("secondaryValue").asText());
        assertEquals("87575.22", revenue.path("fieldValue").asText());
        assertEquals("87575.22", revenue.path("tertiaryValue").asText());
        assertEquals(0, populatedCashFlowValues, "缺失的现金流量表必须保留完整空字段，不能生成模拟数据");
    }

    @Test
    @Transactional
    void enterpriseWithActiveReportsMustBeEmptiedBeforeDeletionAndCanThenBeRecreated() throws Exception {
        String authorization = loginAsAdmin();
        String enterpriseJson = "{\"name\":\"删除恢复测试企业\",\"creditCode\":\"91320100TEST000001\"}";
        MvcResult created = mockMvc.perform(post("/api/enterprises")
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content(enterpriseJson))
                .andExpect(status().isOk()).andReturn();
        long enterpriseId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String archiveJson = "{\"enterpriseId\":" + enterpriseId
                + ",\"reportType\":\"MONTHLY\",\"reportPeriod\":\"2026-09\","
                + "\"reportDate\":\"2026-09-30\",\"year\":2026,\"month\":9,"
                + "\"filingStatus\":\"DRAFT\"}";
        MvcResult firstArchive = mockMvc.perform(post("/api/reports/archive")
                        .header("Authorization", authorization)
                        .contentType("application/json").content(archiveJson))
                .andExpect(status().isOk()).andReturn();
        long firstArchiveId = objectMapper.readTree(firstArchive.getResponse().getContentAsString())
                .path("data").asLong();
        mockMvc.perform(delete("/api/reports/{id}", firstArchiveId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        MvcResult recreatedArchive = mockMvc.perform(post("/api/reports/archive")
                        .header("Authorization", authorization)
                        .contentType("application/json").content(archiveJson))
                .andExpect(status().isOk()).andReturn();
        long recreatedArchiveId = objectMapper.readTree(recreatedArchive.getResponse().getContentAsString())
                .path("data").asLong();

        mockMvc.perform(delete("/api/enterprises/{id}", enterpriseId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
        mockMvc.perform(delete("/api/reports/{id}", recreatedArchiveId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/enterprises/{id}", enterpriseId)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        MvcResult restored = mockMvc.perform(post("/api/enterprises")
                        .header("Authorization", authorization)
                        .contentType("application/json").content(enterpriseJson))
                .andExpect(status().isOk()).andReturn();
        assertEquals(enterpriseId, objectMapper.readTree(restored.getResponse().getContentAsString())
                .path("data").path("id").asLong());
    }

    @Test
    @Transactional
    void demoInitializerDoesNotReinsertSoftDeletedEnterpriseCodes() {
        enterpriseRepository.findAll().forEach(enterprise -> enterprise.setDeleted(1));
        enterpriseRepository.flush();
        long historicalEnterpriseCount = enterpriseRepository.count();

        demoDataInitializer.run(null);

        assertEquals(historicalEnterpriseCount, enterpriseRepository.count());
        assertEquals(0, enterpriseRepository.countByDeleted(0));
    }

    private FinancialReportArchive findReferenceArchive() {
        return archiveRepository.findAll().stream()
                .filter(archive -> "2026-03".equals(archive.getReportPeriod()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("参考报表未初始化"));
    }

    private String loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();
        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return "Bearer " + loginJson.path("data").path("token").asText();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> statementItems(Map<?, ?> statements, String key) {
        Map<String, Object> statement = (Map<String, Object>) statements.get(key);
        return (List<Map<String, Object>>) statement.get("items");
    }

    private void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }
}
