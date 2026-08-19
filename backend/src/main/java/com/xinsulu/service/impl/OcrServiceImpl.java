package com.xinsulu.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinsulu.common.enums.ConfidenceLevel;
import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.entity.OcrFieldResult;
import com.xinsulu.entity.OcrTask;
import com.xinsulu.entity.UploadedFile;
import com.xinsulu.repository.OcrFieldResultRepository;
import com.xinsulu.repository.OcrTaskRepository;
import com.xinsulu.repository.UploadedFileRepository;
import com.xinsulu.service.OcrService;
import com.xinsulu.service.ocr.FinancialReportFieldNormalizer;
import com.xinsulu.service.ocr.FinancialReportExcelExtractor;
import com.xinsulu.service.ocr.FinancialReportPdfExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OCR识别服务实现类。
 * 本地开发可使用 mock，生产环境通过 HTTP 适配器接入实际 OCR 服务。
 *
 * @author xinsulu-team
 */
@Slf4j
@Service
public class OcrServiceImpl implements OcrService {

    /** 任务状态存储 */
    private static final Map<Long, OcrTask> TASK_STATUS_MAP = new ConcurrentHashMap<>();

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinancialReportPdfExtractor financialReportPdfExtractor;

    @Autowired
    private FinancialReportExcelExtractor financialReportExcelExtractor;

    @Autowired
    private FinancialReportFieldNormalizer financialReportFieldNormalizer;

    @Value("${ai.provider:mock}")
    private String aiProvider;

    @Value("${ai.http.endpoint:}")
    private String ocrHttpEndpoint;

    @Value("${ai.http.token:}")
    private String ocrHttpToken;

    @Value("${ai.http.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${ai.http.read-timeout-ms:120000}")
    private int readTimeoutMs;

    @Value("${xinsulu.storage.upload-dir:./uploads}")
    private String uploadDir;

    /** 随机数生成器 */
    private final Random random = new Random();

    /**
     * 执行OCR识别任务（同步方式）
     *
     * @param file 上传的文件
     * @return OCR识别任务（包含识别结果）
     */
    @Override
    @Transactional
    public OcrTask recognize(UploadedFile file) {
        log.info("执行OCR识别任务：fileId={}, fileName={}", file.getId(), file.getOriginalFilename());

        file = getManagedUploadedFile(file.getId());

        // 创建OCR任务记录
        OcrTask task = createOcrTask(file);
        task = ocrTaskRepository.save(task);

        long startedAt = System.currentTimeMillis();
        List<OcrFieldResult> fieldResults = recognizeFields(task, file);
        ocrFieldResultRepository.saveAll(fieldResults);

        // 更新任务统计信息
        updateTaskStatistics(task, fieldResults);
        task.setTaskStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setProcessingTimeMs((int) Math.min(Integer.MAX_VALUE,
                System.currentTimeMillis() - startedAt));

        task = ocrTaskRepository.save(task);
        log.info("OCR识别完成：taskId={}, fields={}", task.getId(), fieldResults.size());

        return task;
    }

    /**
     * 异步执行OCR识别任务
     *
     * @param file 上传的文件
     * @return OCR识别任务（异步处理）
     */
    @Override
    @Transactional
    public OcrTask recognizeAsync(UploadedFile file) {
        log.info("异步执行OCR识别任务：fileId={}", file.getId());

        file = getManagedUploadedFile(file.getId());

        // 创建OCR任务记录
        OcrTask task = createOcrTask(file);
        task.setTaskStatus("PENDING");
        task = ocrTaskRepository.save(task);

        // 存储任务状态
        TASK_STATUS_MAP.put(task.getId(), task);

        Long taskId = task.getId();
        Runnable processing = () -> processAsync(taskId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(processing);
                }
            });
        } else {
            taskExecutor.execute(processing);
        }

        return task;
    }

    /**
     * 查询任务状态
     *
     * @param taskId 任务ID
     * @return 任务对象
     */
    @Override
    public OcrTask getTaskStatus(Long taskId) {
        log.info("查询OCR任务状态：taskId={}", taskId);

        // 先从内存中查找
        OcrTask task = TASK_STATUS_MAP.get(taskId);
        if (task == null) {
            // 从数据库中查找
            task = ocrTaskRepository.findById(taskId).orElse(null);
        }

        if (task == null) {
            throw new RuntimeException("OCR任务不存在");
        }

        return task;
    }

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     */
    @Override
    @Transactional
    public void cancelTask(Long taskId) {
        log.info("取消OCR任务：taskId={}", taskId);

        OcrTask task = ocrTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("OCR任务不存在"));

        if (!"PENDING".equals(task.getTaskStatus()) && !"PROCESSING".equals(task.getTaskStatus())) {
            throw new RuntimeException("任务已完成或已取消，无法取消");
        }

        task.setTaskStatus("CANCELLED");
        task.setErrorMessage("用户手动取消");
        ocrTaskRepository.save(task);

        TASK_STATUS_MAP.remove(taskId);
        log.info("OCR任务已取消：taskId={}", taskId);
    }

    /**
     * 获取OCR任务的识别结果
     *
     * @param taskId 任务ID
     * @return 识别结果列表
     */
    @Override
    public Object getTaskResults(Long taskId) {
        log.info("获取OCR识别结果：taskId={}", taskId);

        OcrTask task = ocrTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("OCR任务不存在"));

        // 查询该任务的所有字段识别结果
        List<OcrFieldResult> results = ocrFieldResultRepository.findByOcrTaskIdOrderByFieldCodeAsc(taskId);

        // 构建返回数据（包含任务信息和字段列表）
        java.util.Map<String, Object> responseData = new java.util.HashMap<>();
        responseData.put("taskId", task.getId());
        responseData.put("taskStatus", task.getTaskStatus());
        responseData.put("totalFields", task.getTotalFields());
        responseData.put("recognizedFields", task.getRecognizedFields());
        responseData.put("highConfidenceCount", task.getHighConfidenceCount());
        responseData.put("mediumConfidenceCount", task.getMediumConfidenceCount());
        responseData.put("lowConfidenceCount", task.getLowConfidenceCount());
        responseData.put("averageConfidence", task.getAverageConfidence());
        responseData.put("resultSummary", task.getResultSummary());
        responseData.put("enterpriseName", task.getSourceEnterpriseName());
        responseData.put("reportPeriod", task.getSourceReportPeriod());
        responseData.put("reportDate", task.getSourceReportDate());
        responseData.put("unit", task.getSourceUnit());
        responseData.put("fieldResults", results);

        return responseData;
    }

    @Override
    @Transactional
    public OcrTask mergeTasks(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new BusinessException("请选择至少一张已识别的报表图片");
        }
        List<OcrTask> tasks = new ArrayList<>();
        for (Long taskId : taskIds) {
            OcrTask task = ocrTaskRepository.findById(taskId)
                    .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                    .orElseThrow(() -> new BusinessException(404, "OCR任务不存在"));
            if (!"COMPLETED".equalsIgnoreCase(task.getTaskStatus())) {
                throw new BusinessException("存在尚未完成的OCR任务，暂不能合并");
            }
            tasks.add(task);
        }
        OcrTask primary = tasks.get(0);
        Map<String, OcrFieldResult> merged = new LinkedHashMap<>();
        List<OcrFieldResult> primaryFields = ocrFieldResultRepository
                .findByOcrTaskIdOrderByFieldCodeAsc(primary.getId());
        primaryFields.stream().filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .forEach(item -> merged.put(fieldMergeKey(item), item));

        for (int index = 1; index < tasks.size(); index++) {
            OcrTask sourceTask = tasks.get(index);
            List<OcrFieldResult> sourceFields = ocrFieldResultRepository
                    .findByOcrTaskIdOrderByFieldCodeAsc(sourceTask.getId());
            for (OcrFieldResult source : sourceFields) {
                if (!Integer.valueOf(0).equals(source.getDeleted())) continue;
                String key = fieldMergeKey(source);
                OcrFieldResult target = merged.get(key);
                if (target == null) {
                    target = cloneForTask(source, primary);
                    primaryFields.add(target);
                    merged.put(key, target);
                } else {
                    mergeRecognizedValues(source, target);
                    target.setUpdatedTime(LocalDateTime.now());
                }
                source.setDeleted(1);
                source.setUpdatedTime(LocalDateTime.now());
            }
            ocrFieldResultRepository.saveAll(sourceFields);
            fillTaskMetadata(primary, sourceTask);
            if (sourceTask.getFile() != null) {
                sourceTask.getFile().setOcrTaskId(primary.getId());
                sourceTask.getFile().setUpdatedTime(LocalDateTime.now());
                uploadedFileRepository.save(sourceTask.getFile());
            }
            sourceTask.setDeleted(1);
            sourceTask.setUpdatedTime(LocalDateTime.now());
            ocrTaskRepository.save(sourceTask);
        }
        primaryFields = ocrFieldResultRepository.saveAll(primaryFields);
        updateTaskStatistics(primary, primaryFields);
        primary.setProvider("BATCH_MERGED");
        primary.setUpdatedTime(LocalDateTime.now());
        return ocrTaskRepository.save(primary);
    }

    private String fieldMergeKey(OcrFieldResult field) {
        String code = field.getFieldCode() == null || field.getFieldCode().trim().isEmpty()
                ? field.getFieldName() : field.getFieldCode();
        return String.valueOf(field.getFieldType()) + "|" + code;
    }

    private double fieldQuality(OcrFieldResult field) {
        boolean hasValue = hasText(field.getFieldValue()) || hasText(field.getSecondaryValue())
                || hasText(field.getTertiaryValue()) || field.getNumericValue() != null;
        return (hasValue ? 100d : 0d)
                + (field.getConfidenceScore() == null ? 0d : field.getConfidenceScore().doubleValue());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private OcrFieldResult cloneForTask(OcrFieldResult source, OcrTask task) {
        OcrFieldResult target = new OcrFieldResult();
        target.setOcrTask(task);
        target.setFieldName(source.getFieldName());
        target.setFieldCode(source.getFieldCode());
        target.setFieldType(source.getFieldType());
        target.setCreatedTime(LocalDateTime.now());
        target.setUpdatedTime(LocalDateTime.now());
        target.setDeleted(0);
        copyRecognizedValues(source, target);
        return target;
    }

    private void copyRecognizedValues(OcrFieldResult source, OcrFieldResult target) {
        target.setFieldValue(source.getFieldValue());
        target.setSecondaryValue(source.getSecondaryValue());
        target.setTertiaryValue(source.getTertiaryValue());
        target.setNumericValue(source.getNumericValue());
        target.setConfidenceScore(source.getConfidenceScore());
        target.setConfidenceLevel(source.getConfidenceLevel());
        target.setIsReviewed(source.getIsReviewed());
        target.setReviewedValue(source.getReviewedValue());
        target.setReviewedBy(source.getReviewedBy());
        target.setReviewedAt(source.getReviewedAt());
        target.setReviewComment(source.getReviewComment());
        target.setBoundingBox(source.getBoundingBox());
        target.setPageNumber(source.getPageNumber());
    }

    private void mergeRecognizedValues(OcrFieldResult source, OcrFieldResult target) {
        double sourceConfidence = source.getConfidenceScore() == null ? 0d : source.getConfidenceScore().doubleValue();
        double targetConfidence = target.getConfidenceScore() == null ? 0d : target.getConfidenceScore().doubleValue();
        if (hasText(source.getFieldValue()) && (!hasText(target.getFieldValue()) || sourceConfidence > targetConfidence)) {
            target.setFieldValue(source.getFieldValue());
            target.setNumericValue(source.getNumericValue());
        }
        if (hasText(source.getSecondaryValue()) && (!hasText(target.getSecondaryValue()) || sourceConfidence > targetConfidence)) {
            target.setSecondaryValue(source.getSecondaryValue());
        }
        if (hasText(source.getTertiaryValue()) && (!hasText(target.getTertiaryValue()) || sourceConfidence > targetConfidence)) {
            target.setTertiaryValue(source.getTertiaryValue());
        }
        if (sourceConfidence > targetConfidence) {
            target.setConfidenceScore(source.getConfidenceScore());
            target.setConfidenceLevel(source.getConfidenceLevel());
            target.setBoundingBox(source.getBoundingBox());
            target.setPageNumber(source.getPageNumber());
        }
    }

    private void fillTaskMetadata(OcrTask primary, OcrTask source) {
        if (!hasText(primary.getSourceEnterpriseName())) primary.setSourceEnterpriseName(source.getSourceEnterpriseName());
        if (!hasText(primary.getSourceReportPeriod())) primary.setSourceReportPeriod(source.getSourceReportPeriod());
        if (primary.getSourceReportDate() == null) primary.setSourceReportDate(source.getSourceReportDate());
        if (!hasText(primary.getSourceUnit())) primary.setSourceUnit(source.getSourceUnit());
    }

    /**
     * 创建OCR任务记录
     */
    private OcrTask createOcrTask(UploadedFile file) {
        if (file.getUploadedBy() == null) {
            throw new BusinessException("上传文件缺少创建人信息，请重新上传");
        }
        OcrTask task = new OcrTask();
        task.setFile(file);
        task.setCreatedBy(file.getUploadedBy());
        task.setTaskStatus("PROCESSING");
        task.setTaskType("FINANCIAL_STATEMENT");
        task.setProvider(isMockProvider() ? "MOCK_OCR" : "HTTP_OCR");
        task.setStartedAt(LocalDateTime.now());
        task.setCreatedTime(LocalDateTime.now());
        task.setUpdatedTime(LocalDateTime.now());
        task.setDeleted(0);
        return task;
    }

    private UploadedFile getManagedUploadedFile(Long fileId) {
        return uploadedFileRepository.findById(fileId)
                .filter(file -> Integer.valueOf(0).equals(file.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "上传文件不存在"));
    }

    /**
     * 模拟OCR处理过程（进度从0%到100%）
     */
    private void simulateOcrProcessing(OcrTask task) {
        int totalSteps = 10;
        for (int i = 1; i <= totalSteps; i++) {
            try {
                Thread.sleep(50); // 模拟处理耗时
                int progress = (int) ((double) i / totalSteps * 100);
                log.debug("OCR处理进度：{}%", progress);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        task.setCompletedAt(LocalDateTime.now());
        task.setProcessingTimeMs(random.nextInt(2000) + 1000); // 1-3秒
    }

    /**
     * 异步处理OCR任务
     */
    protected void processAsync(Long taskId) {
        try {
            OcrTask task = ocrTaskRepository.findById(taskId).orElse(null);
            if (task == null) return;

            // 更新状态为处理中
            task.setTaskStatus("PROCESSING");
            ocrTaskRepository.save(task);

            UploadedFile uploadedFile = uploadedFileRepository.findById(task.getFile().getId())
                    .orElseThrow(() -> new BusinessException(404, "上传文件不存在"));
            long startedAt = System.currentTimeMillis();
            List<OcrFieldResult> fieldResults = recognizeFields(task, uploadedFile);
            ocrFieldResultRepository.saveAll(fieldResults);

            // 更新任务统计
            updateTaskStatistics(task, fieldResults);
            task.setTaskStatus("COMPLETED");
            task.setCompletedAt(LocalDateTime.now());
            task.setProcessingTimeMs((int) Math.min(Integer.MAX_VALUE,
                    System.currentTimeMillis() - startedAt));
            ocrTaskRepository.save(task);

            // 从内存移除
            TASK_STATUS_MAP.remove(taskId);

            log.info("异步OCR任务完成：taskId={}", taskId);
        } catch (Exception e) {
            log.error("异步OCR任务处理失败：taskId={}", taskId, e);
            try {
                OcrTask task = ocrTaskRepository.findById(taskId).orElse(null);
                if (task != null) {
                    task.setTaskStatus("FAILED");
                    task.setErrorMessage(e.getMessage());
                    ocrTaskRepository.save(task);
                    TASK_STATUS_MAP.remove(taskId);
                }
            } catch (Exception ex) {
                log.error("更新任务状态失败", ex);
            }
        }
    }

    private List<OcrFieldResult> recognizeFields(OcrTask task, UploadedFile file) {
        File sourceFile = resolveSourceFile(file);
        if (isExcel(file) && sourceFile.isFile()) {
            try {
                List<OcrFieldResult> excelFields = financialReportExcelExtractor.extract(task, sourceFile);
                task.setProvider("EXCEL_WORKBOOK");
                return financialReportFieldNormalizer.normalize(task, excelFields);
            } catch (Exception e) {
                log.warn("Excel 本地解析失败，将回退到配置的 OCR 提供方：fileId={}, reason={}",
                        file.getId(), e.getMessage());
            }
        }
        if (isPdf(file) && sourceFile.isFile()) {
            try {
                List<OcrFieldResult> pdfFields = financialReportPdfExtractor.extract(task, sourceFile);
                task.setProvider("PDF_TEXT_LAYER");
                return financialReportFieldNormalizer.normalize(task, pdfFields);
            } catch (Exception e) {
                log.warn("PDF 文字层解析失败，将回退到配置的 OCR 提供方：fileId={}, reason={}",
                        file.getId(), e.getMessage());
            }
        }

        List<OcrFieldResult> rawResults;
        if (isMockProvider()) {
            simulateOcrProcessing(task);
            rawResults = generateMockFieldResults(task);
        } else if ("http".equalsIgnoreCase(aiProvider)) {
            rawResults = recognizeWithHttp(task, file);
        } else {
            throw new BusinessException("不支持的 OCR 提供方：" + aiProvider);
        }
        return financialReportFieldNormalizer.normalize(task, rawResults);
    }

    private boolean isMockProvider() {
        return "mock".equalsIgnoreCase(aiProvider);
    }

    /**
     * 调用标准化 HTTP OCR 适配器。服务端接收 multipart/form-data 的 file 字段，
     * 返回 {"fields":[...]} 或 {"data":{"fields":[...]}}。
     */
    private List<OcrFieldResult> recognizeWithHttp(OcrTask task, UploadedFile uploadedFile) {
        if (ocrHttpEndpoint == null || ocrHttpEndpoint.trim().isEmpty()) {
            throw new BusinessException("生产 OCR 未配置，请设置 OCR_HTTP_ENDPOINT");
        }

        File sourceFile = resolveSourceFile(uploadedFile);
        if (!sourceFile.isFile()) {
            throw new BusinessException(404, "上传文件不存在或已被移动");
        }

        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(connectTimeoutMs);
            requestFactory.setReadTimeout(readTimeoutMs);
            RestTemplate restTemplate = new RestTemplate(requestFactory);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (ocrHttpToken != null && !ocrHttpToken.trim().isEmpty()) {
                headers.setBearerAuth(ocrHttpToken.trim());
            }

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(sourceFile) {
                @Override
                public String getFilename() {
                    return uploadedFile.getOriginalFilename();
                }
            });
            body.add("documentType", "FINANCIAL_STATEMENT");

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ocrHttpEndpoint, new HttpEntity<>(body, headers), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.has("data") && root.get("data").isObject() ? root.get("data") : root;
            applyHttpMetadata(task, data);
            JsonNode fields = data.get("fields");
            if (fields == null || !fields.isArray() || fields.size() == 0) {
                throw new BusinessException("OCR 服务未返回可用字段");
            }

            List<OcrFieldResult> results = new ArrayList<>();
            for (JsonNode node : fields) {
                String fieldName = textValue(node, "fieldName");
                String fieldType = textValue(node, "fieldType");
                if (fieldName.isEmpty() || fieldType.isEmpty()) {
                    continue;
                }
                BigDecimal confidence = decimalValue(node, "confidenceScore", new BigDecimal("100"));
                if (confidence.compareTo(BigDecimal.ONE) <= 0) {
                    confidence = confidence.multiply(new BigDecimal("100"));
                }
                confidence = confidence.max(BigDecimal.ZERO)
                        .min(new BigDecimal("100"))
                        .setScale(4, RoundingMode.HALF_UP);

                OcrFieldResult result = new OcrFieldResult();
                result.setOcrTask(task);
                result.setFieldName(fieldName);
                result.setFieldCode(textValue(node, "fieldCode"));
                result.setFieldValue(textValue(node, "fieldValue"));
                result.setConfidenceScore(confidence);
                result.setConfidenceLevel(determineConfidenceLevel(confidence));
                result.setFieldType(fieldType);
                result.setPageNumber(node.hasNonNull("pageNumber") ? node.get("pageNumber").asInt() : null);
                result.setBoundingBox(textValue(node, "boundingBox"));
                result.setIsReviewed(0);
                result.setCreatedTime(LocalDateTime.now());
                result.setUpdatedTime(LocalDateTime.now());
                result.setDeleted(0);
                results.add(result);
            }
            if (results.isEmpty()) {
                throw new BusinessException("OCR 服务返回的字段格式不正确");
            }
            return results;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTTP OCR 调用失败：endpoint={}", ocrHttpEndpoint, e);
            throw new BusinessException("OCR 服务调用失败：" + e.getMessage());
        }
    }

    private File resolveSourceFile(UploadedFile uploadedFile) {
        File path = new File(uploadedFile.getFilePath());
        if (path.isAbsolute()) {
            return path;
        }
        File underUploadDir = new File(uploadDir, uploadedFile.getFilePath());
        return underUploadDir.isFile() ? underUploadDir : path;
    }

    private boolean isPdf(UploadedFile uploadedFile) {
        String mime = uploadedFile.getMimeType();
        String name = uploadedFile.getOriginalFilename();
        return (mime != null && mime.toLowerCase().contains("pdf"))
                || (name != null && name.toLowerCase().endsWith(".pdf"));
    }

    private boolean isExcel(UploadedFile uploadedFile) {
        String mime = uploadedFile.getMimeType();
        String name = uploadedFile.getOriginalFilename();
        String normalizedMime = mime == null ? "" : mime.toLowerCase();
        String normalizedName = name == null ? "" : name.toLowerCase();
        return normalizedMime.contains("spreadsheet")
                || normalizedMime.contains("excel")
                || normalizedName.endsWith(".xls")
                || normalizedName.endsWith(".xlsx");
    }

    private void applyHttpMetadata(OcrTask task, JsonNode data) {
        String enterpriseName = textValue(data, "enterpriseName");
        if (enterpriseName.isEmpty()) enterpriseName = textValue(data, "companyName");
        if (!enterpriseName.isEmpty()) task.setSourceEnterpriseName(enterpriseName);

        String period = textValue(data, "reportPeriod");
        if (period.isEmpty()) period = textValue(data, "statementPeriod");
        String reportDate = textValue(data, "reportDate");
        if (!reportDate.isEmpty()) {
            try {
                task.setSourceReportDate(LocalDate.parse(reportDate.substring(0, 10)));
                if (period.isEmpty()) period = reportDate.substring(0, 7);
            } catch (Exception e) {
                log.warn("HTTP OCR 返回的报表日期格式无法解析：{}", reportDate);
            }
        }
        if (!period.isEmpty()) task.setSourceReportPeriod(period.substring(0, Math.min(7, period.length())));
        String unit = textValue(data, "unit");
        if (!unit.isEmpty()) task.setSourceUnit(unit);
    }

    private String textValue(JsonNode node, String name) {
        return node.hasNonNull(name) ? node.get(name).asText("").trim() : "";
    }

    private BigDecimal decimalValue(JsonNode node, String name, BigDecimal defaultValue) {
        if (!node.hasNonNull(name)) {
            return defaultValue;
        }
        try {
            return new BigDecimal(node.get(name).asText());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 生成模拟的OCR字段结果
     * 包含资产负债表、利润表、现金流量表的字段
     */
    private List<OcrFieldResult> generateMockFieldResults(OcrTask task) {
        List<OcrFieldResult> results = new ArrayList<>();

        // 资产负债表字段
        results.addAll(generateBalanceSheetFields(task));

        // 利润表字段
        results.addAll(generateIncomeStatementFields(task));

        // 现金流量表字段
        results.addAll(generateCashFlowStatementFields(task));

        return results;
    }

    /**
     * 生成资产负债表模拟字段
     */
    private List<OcrFieldResult> generateBalanceSheetFields(OcrTask task) {
        List<OcrFieldResult> fields = new ArrayList<>();
        String[] items = {
                "货币资金", "应收账款", "预付款项", "存货", "流动资产合计",
                "固定资产原值", "累计折旧", "固定资产净值", "资产总计",
                "短期借款", "应付账款", "预收款项", "应付职工薪酬",
                "流动负债合计", "长期借款", "负债合计",
                "实收资本", "未分配利润", "所有者权益合计"
        };

        BigDecimal[] values = {
                new BigDecimal("1500.50"), new BigDecimal("2300.80"),
                new BigDecimal("450.20"), new BigDecimal("1800.30"),
                new BigDecimal("6051.80"), new BigDecimal("5000.00"),
                new BigDecimal("1200.00"), new BigDecimal("3800.00"),
                new BigDecimal("9851.80"), new BigDecimal("2000.00"),
                new BigDecimal("1800.50"), new BigDecimal("600.30"),
                new BigDecimal("350.20"), new BigDecimal("4751.00"),
                new BigDecimal("1000.00"), new BigDecimal("5751.00"),
                new BigDecimal("3000.00"), new BigDecimal("1100.80"),
                new BigDecimal("4100.80")
        };

        for (int i = 0; i < items.length; i++) {
            OcrFieldResult field = new OcrFieldResult();
            field.setOcrTask(task);
            field.setFieldName(items[i]);
            field.setFieldValue(values[i].toString());
            field.setConfidenceScore(generateConfidence(i)); // 部分字段低置信度
            field.setConfidenceLevel(determineConfidenceLevel(field.getConfidenceScore()));
            field.setFieldType("BALANCE_SHEET");
            field.setPageNumber(1);
            field.setBoundingBox("[100," + (i * 30) + ",400," + ((i + 1) * 30) + "]");
            field.setIsReviewed(0);
            field.setCreatedTime(LocalDateTime.now());
            field.setUpdatedTime(LocalDateTime.now());
            field.setDeleted(0);
            fields.add(field);
        }

        return fields;
    }

    /**
     * 生成利润表模拟字段
     */
    private List<OcrFieldResult> generateIncomeStatementFields(OcrTask task) {
        List<OcrFieldResult> fields = new ArrayList<>();
        String[] items = {
                "营业收入", "营业成本", "税金及附加", "销售费用",
                "管理费用", "研发费用", "财务费用", "营业利润",
                "营业外收入", "营业外支出", "利润总额", "所得税费用", "净利润"
        };

        BigDecimal[] values = {
                new BigDecimal("25000.00"), new BigDecimal("18000.00"),
                new BigDecimal("300.00"), new BigDecimal("1500.00"),
                new BigDecimal("2000.00"), new BigDecimal("800.00"),
                new BigDecimal("450.00"), new BigDecimal("1950.00"),
                new BigDecimal("100.00"), new BigDecimal("50.00"),
                new BigDecimal("2000.00"), new BigDecimal("500.00"),
                new BigDecimal("1500.00")
        };

        for (int i = 0; i < items.length; i++) {
            OcrFieldResult field = new OcrFieldResult();
            field.setOcrTask(task);
            field.setFieldName(items[i]);
            field.setFieldValue(values[i].toString());
            field.setConfidenceScore(generateConfidence(i));
            field.setConfidenceLevel(determineConfidenceLevel(field.getConfidenceScore()));
            field.setFieldType("INCOME_STATEMENT");
            field.setPageNumber(2);
            field.setBoundingBox("[100," + (i * 30) + ",400," + ((i + 1) * 30) + "]");
            field.setIsReviewed(0);
            field.setCreatedTime(LocalDateTime.now());
            field.setUpdatedTime(LocalDateTime.now());
            field.setDeleted(0);
            fields.add(field);
        }

        return fields;
    }

    /**
     * 生成现金流量表模拟字段
     */
    private List<OcrFieldResult> generateCashFlowStatementFields(OcrTask task) {
        List<OcrFieldResult> fields = new ArrayList<>();
        String[] items = {
                "销售商品收到的现金", "收到其他与经营活动有关的现金",
                "经营活动现金流入小计", "购买商品支付的现金",
                "支付给职工的现金", "支付的各项税费",
                "经营活动现金流出小计", "经营活动产生的现金流量净额",
                "收回投资收到的现金", "投资活动现金流入小计",
                "购建固定资产支付的现金", "投资活动现金流出小计",
                "投资活动产生的现金流量净额", "吸收投资收到的现金",
                "筹资活动现金流入小计", "偿还债务支付的现金",
                "筹资活动现金流出小计", "筹资活动产生的现金流量净额",
                "现金及现金等价物净增加额"
        };

        BigDecimal[] values = {
                new BigDecimal("26000.00"), new BigDecimal("500.00"),
                new BigDecimal("26500.00"), new BigDecimal("19000.00"),
                new BigDecimal("3000.00"), new BigDecimal("800.00"),
                new BigDecimal("22800.00"), new BigDecimal("3700.00"),
                new BigDecimal("200.00"), new BigDecimal("200.00"),
                new BigDecimal("1500.00"), new BigDecimal("1500.00"),
                new BigDecimal("-1300.00"), new BigDecimal("1000.00"),
                new BigDecimal("1000.00"), new BigDecimal("2000.00"),
                new BigDecimal("2000.00"), new BigDecimal("-1000.00"),
                new BigDecimal("1400.00")
        };

        for (int i = 0; i < items.length; i++) {
            OcrFieldResult field = new OcrFieldResult();
            field.setOcrTask(task);
            field.setFieldName(items[i]);
            field.setFieldValue(values[i].toString());
            field.setConfidenceScore(generateConfidence(i));
            field.setConfidenceLevel(determineConfidenceLevel(field.getConfidenceScore()));
            field.setFieldType("CASH_FLOW_STATEMENT");
            field.setPageNumber(3);
            field.setBoundingBox("[100," + (i * 25) + ",450," + ((i + 1) * 25) + "]");
            field.setIsReviewed(0);
            field.setCreatedTime(LocalDateTime.now());
            field.setUpdatedTime(LocalDateTime.now());
            field.setDeleted(0);
            fields.add(field);
        }

        return fields;
    }

    /**
     * 生成置信度（部分字段低置信度用于演示复核功能）
     */
    private java.math.BigDecimal generateConfidence(int index) {
        // 每5个字段中有1个低置信度字段
        if (index % 5 == 0 && random.nextBoolean()) {
            return java.math.BigDecimal.valueOf(65.0 + random.nextDouble() * 15.0); // 65-80% 低置信度
        } else if (index % 7 == 0) {
            return java.math.BigDecimal.valueOf(80.0 + random.nextDouble() * 10.0); // 80-90% 中等置信度
        } else {
            return java.math.BigDecimal.valueOf(90.0 + random.nextDouble() * 9.9); // 90-99.9% 高置信度
        }
    }

    /**
     * 根据置信度确定置信度等级
     */
    private String determineConfidenceLevel(java.math.BigDecimal confidence) {
        if (confidence.compareTo(new java.math.BigDecimal("90")) >= 0) {
            return ConfidenceLevel.HIGH.name();
        } else if (confidence.compareTo(new java.math.BigDecimal("70")) >= 0) {
            return ConfidenceLevel.MEDIUM.name();
        } else {
            return ConfidenceLevel.LOW.name();
        }
    }

    /**
     * 更新任务的统计信息
     */
    private void updateTaskStatistics(OcrTask task, List<OcrFieldResult> fieldResults) {
        if (fieldResults == null || fieldResults.isEmpty()) {
            throw new BusinessException("OCR 未识别出有效字段");
        }
        task.setTotalFields(fieldResults.size());
        task.setRecognizedFields(fieldResults.size());

        int highCount = 0, mediumCount = 0, lowCount = 0;
        double totalConfidence = 0;

        for (OcrFieldResult result : fieldResults) {
            totalConfidence += result.getConfidenceScore().doubleValue();
            String level = result.getConfidenceLevel();
            if (ConfidenceLevel.HIGH.name().equals(level)) {
                highCount++;
            } else if (ConfidenceLevel.MEDIUM.name().equals(level)) {
                mediumCount++;
            } else {
                lowCount++;
            }
        }

        task.setHighConfidenceCount(highCount);
        task.setMediumConfidenceCount(mediumCount);
        task.setLowConfidenceCount(lowCount);
        task.setAverageConfidence(BigDecimal.valueOf(totalConfidence / fieldResults.size())
                .setScale(4, RoundingMode.HALF_UP));
        task.setResultSummary(String.format("共识别%d个字段，高置信度%d个，中等%d个，低置信度%d个",
                fieldResults.size(), highCount, mediumCount, lowCount));
    }
}
