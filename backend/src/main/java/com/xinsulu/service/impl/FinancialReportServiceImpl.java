package com.xinsulu.service.impl;

import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.common.util.BigDecimalUtil;
import com.xinsulu.dto.OcrFieldReviewDTO;
import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.dto.ReportArchiveDTO;
import com.xinsulu.dto.ReportIntakeDTO;
import com.xinsulu.entity.*;
import com.xinsulu.repository.*;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.AnalysisReportVO;
import com.xinsulu.vo.HealthScoreVO;
import com.xinsulu.vo.IndicatorVO;
import com.xinsulu.vo.ReportDetailVO;
import com.xinsulu.vo.TrendVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务报表服务实现类（核心服务）
 * 提供报表建档、财务指标计算、健康度评分、数据校验、分析报告生成等功能
 *
 * @author xinsulu-team
 */
@Slf4j
@Service
@Transactional
public class FinancialReportServiceImpl implements FinancialReportService {

    @Autowired
    private FinancialReportArchiveRepository archiveRepository;

    @Autowired
    private EnterpriseRepository enterpriseRepository;

    @Autowired
    private BalanceSheetRepository balanceSheetRepository;

    @Autowired
    private BalanceSheetItemRepository balanceSheetItemRepository;

    @Autowired
    private IncomeStatementRepository incomeStatementRepository;

    @Autowired
    private IncomeStatementItemRepository incomeStatementItemRepository;

    @Autowired
    private CashFlowStatementRepository cashFlowStatementRepository;

    @Autowired
    private CashFlowStatementItemRepository cashFlowStatementItemRepository;

    @Autowired
    private OcrTaskRepository ocrTaskRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private OcrFieldResultRepository ocrFieldResultRepository;

    @Autowired
    private FinancialIndicatorValueRepository indicatorValueRepository;

    @Autowired
    private FinancialHealthScoreRepository healthScoreRepository;

    @Autowired
    private FinancialAnalysisReportRepository analysisReportRepository;

    @Autowired
    private HistoricalIndicatorValueRepository historicalIndicatorValueRepository;

    @Autowired
    private IndicatorRuleConfigRepository indicatorRuleConfigRepository;

    @Autowired
    private HealthWeightConfigRepository healthWeightConfigRepository;

    @Autowired
    private UserRepository userRepository;

    /** 计算精度 */
    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    /**
     * 创建报表归档记录
     *
     * @param reportArchiveDTO 报表归档信息
     * @return 归档ID
     */
    @Override
    @Transactional
    public Long createArchive(ReportArchiveDTO reportArchiveDTO) {
        log.info("创建报表归档：enterpriseId={}, period={}",
                reportArchiveDTO.getEnterpriseId(), reportArchiveDTO.getReportPeriod());

        // 验证企业是否存在
        Enterprise enterprise = enterpriseRepository.findById(reportArchiveDTO.getEnterpriseId())
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "企业不存在"));

        UploadedFile uploadedFile = null;
        if (reportArchiveDTO.getFileId() != null) {
            uploadedFile = uploadedFileRepository.findById(reportArchiveDTO.getFileId())
                    .filter(file -> Integer.valueOf(0).equals(file.getDeleted()))
                    .orElseThrow(() -> new BusinessException(404, "上传文件不存在"));
        }
        String managerName = reportArchiveDTO.getManagerName();
        if ((managerName == null || managerName.trim().isEmpty()) && uploadedFile != null
                && uploadedFile.getUploadedBy() != null) {
            managerName = uploadedFile.getUploadedBy().getRealName();
        }

        archiveRepository.findFirstByEnterpriseIdAndReportPeriodAndDeleted(
                        reportArchiveDTO.getEnterpriseId(), reportArchiveDTO.getReportPeriod(), 0)
                .ifPresent(existing -> {
                    throw new BusinessException("该企业在当前报表期间已归档，请勿重复建档");
                });

        // 创建归档记录
        FinancialReportArchive archive = new FinancialReportArchive();
        archive.setEnterprise(enterprise);
        archive.setReportType(reportArchiveDTO.getReportType());
        archive.setReportPeriod(reportArchiveDTO.getReportPeriod());
        archive.setReportYear(reportArchiveDTO.getYear() != null ? reportArchiveDTO.getYear() :
                LocalDate.now().getYear());
        archive.setReportQuarter(reportArchiveDTO.getQuarter());
        archive.setReportMonth(reportArchiveDTO.getMonth());
        archive.setFilingStatus(reportArchiveDTO.getFilingStatus() != null ?
                reportArchiveDTO.getFilingStatus() : "DRAFT");
        archive.setValidationStatus(reportArchiveDTO.getValidationStatus());
        archive.setDataSource(reportArchiveDTO.getDataSource());
        archive.setDataQualityScore(reportArchiveDTO.getDataQualityScore());
        archive.setRemarks(reportArchiveDTO.getRemark());
        archive.setManagerName(managerName == null ? null : managerName.trim());
        archive.setUploadedBy(uploadedFile == null ? null : uploadedFile.getUploadedBy());
        archive.setCreatedTime(LocalDateTime.now());
        archive.setUpdatedTime(LocalDateTime.now());
        archive.setDeleted(0);

        archive = archiveRepository.save(archive);
        if (uploadedFile != null) {
            uploadedFile.setArchive(archive);
            if (reportArchiveDTO.getOcrTaskId() != null) {
                OcrTask ocrTask = ocrTaskRepository.findById(reportArchiveDTO.getOcrTaskId())
                        .orElseThrow(() -> new BusinessException(404, "OCR任务不存在"));
                if (ocrTask.getFile() == null || !uploadedFile.getId().equals(ocrTask.getFile().getId())) {
                    throw new BusinessException("OCR任务与上传文件不匹配");
                }
                uploadedFile.setOcrTaskId(ocrTask.getId());
                List<UploadedFile> batchFiles = uploadedFileRepository.findByOcrTaskId(ocrTask.getId());
                for (UploadedFile batchFile : batchFiles) {
                    if (!Integer.valueOf(0).equals(batchFile.getDeleted())) continue;
                    batchFile.setArchive(archive);
                    batchFile.setUpdatedTime(LocalDateTime.now());
                }
                uploadedFileRepository.saveAll(batchFiles);
            }
            uploadedFile.setUpdatedTime(LocalDateTime.now());
            uploadedFileRepository.save(uploadedFile);
        }

        if (reportArchiveDTO.getReportDate() != null) {
            enterprise.setLastReportDate(reportArchiveDTO.getReportDate());
        }
        if (managerName != null && !managerName.trim().isEmpty()) {
            enterprise.setManagerName(managerName.trim());
        }
        enterprise.setUpdatedTime(LocalDateTime.now());
        enterpriseRepository.save(enterprise);
        log.info("报表归档创建成功：id={}", archive.getId());

        return archive.getId();
    }

    @Override
    @Transactional
    public Long createIntake(ReportIntakeDTO intakeDTO) {
        if (intakeDTO == null || intakeDTO.getEnterprise() == null || intakeDTO.getReport() == null) {
            throw new BusinessException("企业资料和报表归档资料不能为空");
        }
        com.xinsulu.dto.EnterpriseDTO draft = intakeDTO.getEnterprise();
        ReportArchiveDTO report = intakeDTO.getReport();
        if (draft.getName() == null || draft.getName().trim().isEmpty()
                || draft.getCreditCode() == null || draft.getCreditCode().trim().isEmpty()) {
            throw new BusinessException("企业名称和统一社会信用代码不能为空");
        }
        if (report.getFileId() == null || report.getOcrTaskId() == null) {
            throw new BusinessException("请先完成文件上传和OCR识别");
        }
        OcrTask task = ocrTaskRepository.findById(report.getOcrTaskId())
                .filter(item -> Integer.valueOf(0).equals(item.getDeleted()))
                .orElseThrow(() -> new BusinessException(404, "OCR任务不存在"));
        if (!"COMPLETED".equalsIgnoreCase(task.getTaskStatus())) {
            throw new BusinessException("OCR识别尚未完成，不能创建报表归档");
        }
        if (task.getFile() == null || !report.getFileId().equals(task.getFile().getId())) {
            throw new BusinessException("OCR任务与上传文件不匹配");
        }

        String code = draft.getCreditCode().trim().toUpperCase();
        Enterprise enterprise = enterpriseRepository.findByEnterpriseCodeIgnoreCase(code).orElse(null);
        boolean existingActiveEnterprise = enterprise != null && Integer.valueOf(0).equals(enterprise.getDeleted());
        if (enterprise == null) {
            enterprise = new Enterprise();
            enterprise.setEnterpriseCode(code);
            enterprise.setCreatedTime(LocalDateTime.now());
        }
        if (!existingActiveEnterprise) {
            enterprise.setEnterpriseName(draft.getName().trim());
        }
        enterprise.setIndustry(draft.getIndustry());
        enterprise.setLegalPerson(draft.getLegalPerson());
        enterprise.setRegisteredCapital(draft.getRegisteredCapital());
        enterprise.setAddress(draft.getAddress());
        enterprise.setContactPhone(draft.getPhone());
        enterprise.setManagerName(draft.getManagerName() == null ? null : draft.getManagerName().trim());
        enterprise.setDeleted(0);
        enterprise.setUpdatedTime(LocalDateTime.now());
        enterprise = enterpriseRepository.save(enterprise);

        report.setEnterpriseId(enterprise.getId());
        if (report.getReportPeriod() == null || report.getReportPeriod().trim().isEmpty()) {
            report.setReportPeriod(task.getSourceReportPeriod());
        }
        if (report.getReportDate() == null) report.setReportDate(task.getSourceReportDate());
        if (report.getReportDate() == null && report.getReportPeriod() != null
                && report.getReportPeriod().matches("\\d{4}-\\d{2}")) {
            report.setReportDate(YearMonth.parse(report.getReportPeriod()).atEndOfMonth());
        }
        if (report.getReportPeriod() == null || report.getReportPeriod().trim().isEmpty()
                || report.getReportDate() == null) {
            throw new BusinessException("未识别到报表期，请补充报表期后重试");
        }
        report.setYear(report.getReportDate().getYear());
        report.setMonth(report.getReportDate().getMonthValue());
        report.setReportType(report.getReportType() == null ? "MONTHLY" : report.getReportType());
        report.setDataSource(report.getDataSource() == null ? "OCR_AUTO" : report.getDataSource());
        report.setFilingStatus("DRAFT");
        report.setManagerName(draft.getManagerName());
        report.setRemark(draft.getRemark());
        return createArchive(report);
    }

    /**
     * 查询报表详情（包含三大表摘要）
     *
     * @param archiveId 归档ID
     * @return 报表详情
     */
    @Override
    public ReportDetailVO getReportDetail(Long archiveId) {
        log.info("查询报表详情：archiveId={}", archiveId);

        FinancialReportArchive archive = getArchiveById(archiveId);
        ReportDetailVO vo = new ReportDetailVO();

        // 基本信息
        vo.setArchiveId(archive.getId());
        vo.setEnterpriseId(archive.getEnterprise().getId());
        vo.setEnterpriseName(archive.getEnterprise().getEnterpriseName());
        vo.setEnterpriseCreditCode(archive.getEnterprise().getEnterpriseCode());
        vo.setEnterpriseIndustry(archive.getEnterprise().getIndustry());
        vo.setReportPeriod(archive.getReportPeriod());
        vo.setReportDate(resolveReportDate(archive));
        vo.setReportType(archive.getReportType());
        vo.setDataSource(archive.getDataSource());
        vo.setValidationStatus(archive.getValidationStatus());
        vo.setDataQualityScore(archive.getDataQualityScore());
        vo.setFilingStatus(archive.getFilingStatus());
        vo.setReviewComment(archive.getReviewComment());
        vo.setManagerName(archive.getManagerName() != null
                ? archive.getManagerName() : archive.getEnterprise().getManagerName());
        vo.setTotalAssets(archive.getTotalAssets());
        vo.setTotalLiabilities(archive.getTotalLiabilities());
        vo.setTotalEquity(archive.getTotalEquity());
        vo.setTotalRevenue(archive.getRevenue());
        vo.setNetProfit(archive.getNetProfit());
        vo.setNetOperatingCashFlow(archive.getOperatingCashFlow());
        vo.setCreatedTime(archive.getCreatedTime());
        uploadedFileRepository.findFirstByArchiveIdOrderByCreatedTimeDesc(archiveId)
                .ifPresent(file -> vo.setOcrTaskId(file.getOcrTaskId()));
        healthScoreRepository.findFirstByReportIdAndDeletedOrderByIdDesc(archiveId, 0)
                .ifPresent(score -> {
                    vo.setHealthScoreId(score.getId());
                    vo.setHealthScore(score.getTotalScore());
                    vo.setRiskLevel(score.getRiskLevel());
                });
        analysisReportRepository.findFirstByReportIdAndDeletedOrderByVersionDesc(archiveId, 0)
                .ifPresent(analysis -> {
                    vo.setApprovalStatus(analysis.getStatus());
                    vo.setApprovalSubmittedBy(analysis.getSubmittedBy());
                    vo.setApprovalSubmittedTime(analysis.getSubmittedTime());
                    vo.setApprovedBy(analysis.getApprovedBy());
                    vo.setApprovedTime(analysis.getApprovedTime());
                });

        // 计算资产负债表平衡差额
        if (archive.getTotalAssets() != null && archive.getTotalLiabilities() != null
                && archive.getTotalEquity() != null) {
            BigDecimal expected = archive.getTotalLiabilities().add(archive.getTotalEquity());
            vo.setBalanceDifference(archive.getTotalAssets().subtract(expected)
                    .setScale(2, RoundingMode.HALF_UP));
        }

        Map<String, BigDecimal> incomeData = extractIncomeStatementData(archiveId);
        BigDecimal revenue = incomeData.get("营业收入") != null
                ? incomeData.get("营业收入") : archive.getRevenue();
        BigDecimal cost = incomeData.get("营业成本");
        vo.setTotalRevenue(revenue);
        vo.setTotalCost(cost);
        if (revenue != null && cost != null) {
            vo.setGrossProfitRate(BigDecimalUtil.percentage(revenue.subtract(cost), revenue));
        }

        return vo;
    }

    /**
     * 分页查询企业的报表列表
     *
     * @param enterpriseId 企业ID
     * @param pageQueryDTO 分页参数
     * @return 报表分页列表
     */
    @Override
    public PageResponse<ReportDetailVO> getReportsByEnterprise(Long enterpriseId, PageQueryDTO pageQueryDTO) {
        return getReports(enterpriseId, null, null, pageQueryDTO);
    }

    @Override
    public PageResponse<ReportDetailVO> getReports(Long enterpriseId, String period, String status,
                                                    PageQueryDTO pageQueryDTO) {
        log.info("查询报表列表：enterpriseId={}, period={}, status={}", enterpriseId, period, status);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        PageRequest pageRequest = PageRequest.of(
                pageQueryDTO.getPageNum() - 1,
                pageQueryDTO.getPageSize(),
                sort
        );

        Specification<FinancialReportArchive> specification = (root, query, criteriaBuilder) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("deleted"), 0));
            predicates.add(criteriaBuilder.equal(root.get("enterprise").get("deleted"), 0));
            if (enterpriseId != null) {
                predicates.add(criteriaBuilder.equal(root.get("enterprise").get("id"), enterpriseId));
            }
            if (period != null && !period.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("reportPeriod"), period.trim()));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("filingStatus"), status.trim()));
            }
            if (pageQueryDTO.getKeyword() != null && !pageQueryDTO.getKeyword().trim().isEmpty()) {
                String keyword = "%" + pageQueryDTO.getKeyword().trim() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(root.get("enterprise").get("enterpriseName"), keyword),
                        criteriaBuilder.like(root.get("enterprise").get("enterpriseCode"), keyword)));
            }
            if (pageQueryDTO.getRiskLevel() != null && !pageQueryDTO.getRiskLevel().trim().isEmpty()) {
                javax.persistence.criteria.Subquery<Long> riskQuery = query.subquery(Long.class);
                javax.persistence.criteria.Root<FinancialHealthScore> health =
                        riskQuery.from(FinancialHealthScore.class);
                riskQuery.select(health.get("report").get("id"))
                        .where(
                                criteriaBuilder.equal(health.get("report").get("id"), root.get("id")),
                                criteriaBuilder.equal(health.get("deleted"), 0),
                                criteriaBuilder.equal(health.get("riskLevel"),
                                        pageQueryDTO.getRiskLevel().trim()));
                predicates.add(criteriaBuilder.exists(riskQuery));
            }
            return criteriaBuilder.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
        Page<FinancialReportArchive> page = archiveRepository.findAll(specification, pageRequest);

        List<ReportDetailVO> voList = page.getContent().stream()
                .map(archive -> getReportDetail(archive.getId()))
                .collect(Collectors.toList());

        return PageResponse.of(voList, page.getTotalElements(),
                pageQueryDTO.getPageNum(), pageQueryDTO.getPageSize());
    }

    /**
     * 删除报表归档及关联数据
     *
     * @param archiveId 归档ID
     */
    @Override
    @Transactional
    public void deleteArchive(Long archiveId) {
        log.info("删除报表归档：archiveId={}", archiveId);
        FinancialReportArchive archive = getArchiveById(archiveId);
        LocalDateTime now = LocalDateTime.now();
        archive.setDeleted(1);
        archive.setUpdatedTime(now);
        archiveRepository.save(archive);

        for (UploadedFile file : uploadedFileRepository.findByArchiveId(archiveId)) {
            file.setDeleted(1);
            file.setUpdatedTime(now);
            uploadedFileRepository.save(file);
            for (OcrTask task : ocrTaskRepository.findByFileId(file.getId())) {
                task.setDeleted(1);
                task.setUpdatedTime(now);
                ocrTaskRepository.save(task);
                List<OcrFieldResult> taskFields = ocrFieldResultRepository
                        .findByOcrTaskIdOrderByFieldCodeAsc(task.getId());
                for (OcrFieldResult field : taskFields) {
                    field.setDeleted(1);
                    field.setUpdatedTime(now);
                }
                ocrFieldResultRepository.saveAll(taskFields);
            }
        }
        log.info("报表归档删除成功：archiveId={}", archiveId);
    }

    /**
     * 更新报表填报状态
     *
     * @param archiveId 归档ID
     * @param status    状态
     */
    @Override
    @Transactional
    public void updateFilingStatus(Long archiveId, String status) {
        log.info("更新报表状态：archiveId={}, status={}", archiveId, status);
        FinancialReportArchive archive = getArchiveById(archiveId);
        archive.setFilingStatus(status);
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
    }

    /**
     * 字段复核与更新
     *
     * @param reportId 报表ID
     * @param reviews  复核结果列表
     */
    @Transactional
    public void reviewOcrFields(Long reportId, List<OcrFieldReviewDTO> reviews) {
        log.info("字段复核：reportId={}, fields={}", reportId, reviews.size());

        UploadedFile sourceFile = uploadedFileRepository.findFirstByArchiveIdOrderByCreatedTimeDesc(reportId)
                .orElseThrow(() -> new BusinessException("报表未关联上传文件"));
        if (sourceFile.getOcrTaskId() == null) {
            throw new BusinessException("报表未关联OCR任务");
        }

        for (OcrFieldReviewDTO review : reviews) {
            OcrFieldResult fieldResult = ocrFieldResultRepository.findById(review.getFieldResultId())
                    .orElse(null);

            if (fieldResult != null && fieldResult.getOcrTask() != null
                    && sourceFile.getOcrTaskId().equals(fieldResult.getOcrTask().getId())) {
                // 更新复核信息
                if (review.getCorrectedValue() != null) {
                    fieldResult.setFieldValue(review.getCorrectedValue());
                    fieldResult.setIsReviewed(1);
                }
                if (review.getCorrectedSecondaryValue() != null) {
                    fieldResult.setSecondaryValue(review.getCorrectedSecondaryValue());
                    fieldResult.setIsReviewed(1);
                }
                if (review.getCorrectedTertiaryValue() != null) {
                    fieldResult.setTertiaryValue(review.getCorrectedTertiaryValue());
                    fieldResult.setIsReviewed(1);
                }
                if (review.getIsConfirmedCorrect() != null && review.getIsConfirmedCorrect()) {
                    fieldResult.setIsReviewed(1);
                }
                if (review.getConfidence() != null) {
                    fieldResult.setConfidenceScore(java.math.BigDecimal.valueOf(review.getConfidence()));
                }
                fieldResult.setReviewComment(review.getReviewComment());
                fieldResult.setUpdatedTime(LocalDateTime.now());
                ocrFieldResultRepository.save(fieldResult);
            }
        }

        List<OcrFieldResult> recognizedFields =
                ocrFieldResultRepository.findByOcrTaskIdOrderByFieldCodeAsc(sourceFile.getOcrTaskId());
        persistReviewedOcrData(reportId, recognizedFields);
        calculateIndicators(reportId);
        calculateHealthScore(reportId);
        List<Map<String, Object>> validations = validateFinancialData(reportId);
        boolean allPassed = !validations.isEmpty() && validations.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.get("passed")));
        FinancialReportArchive reviewed = getArchiveById(reportId);
        reviewed.setFilingStatus("REVIEWED");
        reviewed.setValidationStatus(allPassed ? "PASSED" : "WARNING");
        reviewed.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(reviewed);
        log.info("字段复核完成");
    }

    /**
     * 保存资产负债表数据
     *
     * @param reportId 报表ID
     * @param items    资产负债表明细项
     */
    @Transactional
    public void saveBalanceSheet(Long reportId, List<BalanceSheetItem> items) {
        log.info("保存资产负债表：reportId={}, items={}", reportId, items.size());

        FinancialReportArchive archive = getArchiveById(reportId);

        // 创建或更新资产负债表主记录
        BalanceSheet balanceSheet = balanceSheetRepository.findByArchiveId(reportId).orElse(null);
        if (balanceSheet == null) {
            balanceSheet = new BalanceSheet();
            balanceSheet.setArchive(archive);
            balanceSheet.setEnterprise(archive.getEnterprise());
            balanceSheet.setReportPeriod(archive.getReportPeriod());
            balanceSheet.setReportDate(resolveReportDate(archive));
            balanceSheet.setCreatedTime(LocalDateTime.now());
            balanceSheet.setDeleted(0);
        }
        balanceSheet.setUpdatedTime(LocalDateTime.now());
        balanceSheet = balanceSheetRepository.save(balanceSheet);

        balanceSheetItemRepository.deleteByBalanceSheetId(balanceSheet.getId());
        for (BalanceSheetItem item : items) {
            item.setBalanceSheet(balanceSheet);
            item.setId(null); // 确保新增
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            item.setDeleted(0);
        }
        balanceSheetItemRepository.saveAll(items);

        // 更新汇总数据到归档记录
        updateArchiveSummaryFromBalanceSheet(archive, items);

        log.info("资产负债表保存完成");
    }

    /**
     * 保存利润表数据
     *
     * @param reportId 报表ID
     * @param items    利润表明细项
     */
    @Transactional
    public void saveIncomeStatement(Long reportId, List<IncomeStatementItem> items) {
        log.info("保存利润表：reportId={}, items={}", reportId, items.size());

        FinancialReportArchive archive = getArchiveById(reportId);

        IncomeStatement incomeStatement = incomeStatementRepository.findByArchiveId(reportId).orElse(null);
        if (incomeStatement == null) {
            incomeStatement = new IncomeStatement();
            incomeStatement.setArchive(archive);
            incomeStatement.setEnterprise(archive.getEnterprise());
            incomeStatement.setReportPeriod(archive.getReportPeriod());
            incomeStatement.setStartDate(LocalDate.of(archive.getReportYear(), 1, 1));
            incomeStatement.setEndDate(resolveReportDate(archive));
            incomeStatement.setCreatedTime(LocalDateTime.now());
            incomeStatement.setDeleted(0);
        }
        incomeStatement.setUpdatedTime(LocalDateTime.now());
        incomeStatement = incomeStatementRepository.save(incomeStatement);

        incomeStatementItemRepository.deleteByIncomeStatementId(incomeStatement.getId());
        for (IncomeStatementItem item : items) {
            item.setIncomeStatement(incomeStatement);
            item.setId(null);
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            item.setDeleted(0);
        }
        incomeStatementItemRepository.saveAll(items);

        // 更新汇总数据到归档记录
        updateArchiveSummaryFromIncomeStatement(archive, items);

        log.info("利润表保存完成");
    }

    /**
     * 保存现金流量表数据
     *
     * @param reportId 报表ID
     * @param items    现金流量表明细项
     */
    @Transactional
    public void saveCashFlowStatement(Long reportId, List<CashFlowStatementItem> items) {
        log.info("保存现金流量表：reportId={}, items={}", reportId, items.size());

        FinancialReportArchive archive = getArchiveById(reportId);

        CashFlowStatement cashFlowStatement = cashFlowStatementRepository.findByArchiveId(reportId).orElse(null);
        if (cashFlowStatement == null) {
            cashFlowStatement = new CashFlowStatement();
            cashFlowStatement.setArchive(archive);
            cashFlowStatement.setEnterprise(archive.getEnterprise());
            cashFlowStatement.setReportPeriod(archive.getReportPeriod());
            cashFlowStatement.setReportDate(resolveReportDate(archive));
            cashFlowStatement.setCreatedTime(LocalDateTime.now());
            cashFlowStatement.setDeleted(0);
        }
        cashFlowStatement.setUpdatedTime(LocalDateTime.now());
        cashFlowStatement = cashFlowStatementRepository.save(cashFlowStatement);

        cashFlowStatementItemRepository.deleteByStatementId(cashFlowStatement.getId());
        for (CashFlowStatementItem item : items) {
            item.setStatement(cashFlowStatement);
            item.setId(null);
            item.setCreatedTime(LocalDateTime.now());
            item.setUpdatedTime(LocalDateTime.now());
            item.setDeleted(0);
        }
        cashFlowStatementItemRepository.saveAll(items);
        updateArchiveSummaryFromCashFlowStatement(archive, items);

        log.info("现金流量表保存完成");
    }

    /**
     * 财务指标计算（核心方法）
     * 实现30+个财务指标公式
     *
     * @param reportId 报表ID
     * @return 财务指标Map
     */
    public Map<String, Object> calculateIndicators(Long reportId) {
        log.info("计算财务指标：reportId={}", reportId);

        FinancialReportArchive archive = getArchiveById(reportId);

        // 获取三大报表数据
        Map<String, BigDecimal> bsData = extractBalanceSheetData(reportId);
        Map<String, BigDecimal> bsBeginningData = extractBalanceSheetBeginningData(reportId);
        Map<String, BigDecimal> isData = extractIncomeStatementData(reportId);
        Map<String, BigDecimal> cfData = extractCashFlowData(reportId);

        Map<String, Object> indicators = new LinkedHashMap<>();

        // ========== 短期偿债能力指标（5个）==========
        indicators.putAll(calculateSolvencyShortTerm(bsData));

        // ========== 长期偿债能力指标（6个）==========
        indicators.putAll(calculateSolvencyLongTerm(bsData, isData));

        // ========== 盈利能力指标（7个）==========
        indicators.putAll(calculateProfitability(isData, bsData, bsBeginningData));

        // ========== 运营效率指标（7个）==========
        indicators.putAll(calculateOperationEfficiency(isData, bsData, bsBeginningData));

        // ========== 现金流能力指标（6个）==========
        indicators.putAll(cashFlowAbility(cfData, bsData, isData));

        // ========== 成长能力指标（5个）==========
        indicators.putAll(calculateGrowthAbility(reportId, isData, bsData, bsBeginningData, cfData));

        // 保存指标值到数据库
        saveIndicatorValues(reportId, indicators);

        log.info("财务指标计算完成：count={}", indicators.size());
        return indicators;
    }

    /**
     * 计算短期偿债能力指标
     */
    private Map<String, Object> calculateSolvencyShortTerm(Map<String, BigDecimal> data) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal currentAssets = data.get("流动资产合计");
        BigDecimal currentLiabilities = data.get("流动负债合计");
        BigDecimal inventory = data.get("存货");
        BigDecimal monetaryCapital = data.get("货币资金");
        BigDecimal notesReceivable = data.get("应收票据");
        BigDecimal accountsReceivable = data.get("应收账款");

        // 流动比率 = 流动资产 / 流动负债
        result.put("currentRatio", safeDivide(currentAssets, currentLiabilities));

        // 速动比率 = (流动资产 - 存货) / 流动负债
        BigDecimal quickAssets = BigDecimalUtil.subtract(currentAssets, inventory);
        result.put("quickRatio", safeDivide(quickAssets, currentLiabilities));

        // 保守速动比率 = (货币资金 + 应收票据 + 应收账款) / 流动 liabilities
        BigDecimal conservativeQuickAssets = BigDecimalUtil.add(monetaryCapital, notesReceivable, accountsReceivable);
        result.put("conservativeQuickRatio", safeDivide(conservativeQuickAssets, currentLiabilities));

        // 现金比率 = 货币资金 / 流动负债
        result.put("cashRatio", safeDivide(monetaryCapital, currentLiabilities));

        // 营运资本 = 流动资产 - 流动负债
        result.put("workingCapital", BigDecimalUtil.subtract(currentAssets, currentLiabilities));

        return result;
    }

    /**
     * 计算长期偿债能力指标
     */
    private Map<String, Object> calculateSolvencyLongTerm(Map<String, BigDecimal> bsData,
                                                           Map<String, BigDecimal> isData) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal totalAssets = bsData.get("资产总计");
        BigDecimal totalLiabilities = bsData.get("负债合计");
        BigDecimal totalEquity = bsData.get("所有者权益合计");
        BigDecimal intangibleAssets = bsData.get("无形资产");
        BigDecimal nonCurrentLiabilities = bsData.get("非流动负债合计");
        BigDecimal currentAssets = bsData.get("流动资产合计");
        BigDecimal currentLiabilities = bsData.get("流动负债合计");

        BigDecimal totalProfit = isData.get("利润总额");
        BigDecimal interestExpense = isData.get("利息支出");

        // 资产负债率 = 总负债 / 总资产 * 100%
        result.put("debtToAssetRatio", percentage(totalLiabilities, totalAssets));

        // 产权比率 = 总负债 / 所有者权益
        result.put("debtToEquityRatio", safeDivide(totalLiabilities, totalEquity));

        // 有形净值债务率 = 总负债 / (所有者权益 - 无形资产)
        BigDecimal tangibleNetWorth = BigDecimalUtil.subtract(totalEquity, intangibleAssets);
        result.put("tangibleDebtRatio", safeDivide(totalLiabilities, tangibleNetWorth));

        // 利息保障倍数 = (利润总额 + 利息支出) / 利息支出
        BigDecimal ebit = BigDecimalUtil.add(totalProfit, interestExpense);
        result.put("interestCoverageRatio", safeDivide(ebit, interestExpense));

        // 长期债务与营运资金比率 = 非流动负债 / (流动资产 - 流动负债)
        BigDecimal workingCapital = BigDecimalUtil.subtract(currentAssets, currentLiabilities);
        result.put("longTermDebtToWorkingCapital", safeDivide(nonCurrentLiabilities, workingCapital));

        return result;
    }

    /**
     * 计算盈利能力指标
     */
    private Map<String, Object> calculateProfitability(Map<String, BigDecimal> isData,
                                                        Map<String, BigDecimal> bsData,
                                                        Map<String, BigDecimal> bsBeginningData) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal revenue = isData.get("营业收入");
        BigDecimal cost = isData.get("营业成本");
        BigDecimal operatingProfit = isData.get("营业利润");
        BigDecimal netProfit = isData.get("净利润");
        BigDecimal totalProfit = isData.get("利润总额");
        BigDecimal taxAndSurcharges = isData.get("税金及附加");
        BigDecimal salesExpenses = isData.get("销售费用");
        BigDecimal adminExpenses = isData.get("管理费用");
        BigDecimal financeExpenses = isData.get("财务费用");
        BigDecimal rdExpenses = isData.get("研发费用");

        BigDecimal totalAssets = bsData.get("资产总计");
        BigDecimal totalEquity = bsData.get("所有者权益合计");

        // 销售毛利率 = (营业收入 - 营业成本) / 营业收入 * 100%
        BigDecimal grossProfit = BigDecimalUtil.subtract(revenue, cost);
        result.put("grossProfitMargin", percentage(grossProfit, revenue));

        // 营业利润率 = 营业利润 / 营业收入 * 100%
        result.put("operatingProfitMargin", percentage(operatingProfit, revenue));

        // 销售净利率 = 净利润 / 营业收入 * 100%
        result.put("netProfitMargin", percentage(netProfit, revenue));

        // 成本费用利润率 = 利润总额 / (营业成本 + 税金及附加 + 销售费用 + 管理费用 + 财务费用) * 100%
        BigDecimal totalCostAndExpenses = BigDecimalUtil.add(cost, taxAndSurcharges,
                salesExpenses, adminExpenses, financeExpenses);
        result.put("costExpenseProfitMargin", percentage(totalProfit, totalCostAndExpenses));

        // 总资产收益率ROA = 净利润 / 平均总资产 * 100%（简化：使用期末总资产）
        result.put("roa", percentage(netProfit,
                average(totalAssets, bsBeginningData.get("资产总计"))));

        // 权益为负时ROE会产生方向误导，按模型数据字典标记为不可计算。
        BigDecimal averageEquity = average(totalEquity, bsBeginningData.get("所有者权益合计"));
        result.put("roe", averageEquity != null && averageEquity.compareTo(BigDecimal.ZERO) > 0
                ? percentage(netProfit, averageEquity)
                : null);

        return result;
    }

    /**
     * 计算运营效率指标
     */
    private Map<String, Object> calculateOperationEfficiency(Map<String, BigDecimal> isData,
                                                              Map<String, BigDecimal> bsData,
                                                              Map<String, BigDecimal> bsBeginningData) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal revenue = isData.get("营业收入");
        BigDecimal cost = isData.get("营业成本");
        BigDecimal accountsReceivable = bsData.get("应收账款");
        BigDecimal inventory = bsData.get("存货");
        BigDecimal currentAssets = bsData.get("流动资产合计");
        BigDecimal fixedAssets = bsData.get("固定资产净值");
        BigDecimal totalAssets = bsData.get("资产总计");

        // 应收账款周转率 = 营业收入 / 平均应收账款（简化使用期末值）
        result.put("accountsReceivableTurnover", safeDivide(revenue,
                average(accountsReceivable, bsBeginningData.get("应收账款"))));

        // 应收账款周转天数 = 365 / 应收账款周转率
        BigDecimal art = safeDivide(revenue,
                average(accountsReceivable, bsBeginningData.get("应收账款")));
        result.put("accountsReceivableTurnoverDays",
                art != null ? new BigDecimal("365").divide(art, SCALE, ROUNDING_MODE) : null);

        // 存货周转率 = 营业成本 / 平均存货
        result.put("inventoryTurnover", safeDivide(cost,
                average(inventory, bsBeginningData.get("存货"))));

        // 存货周转天数 = 365 / 存货周转率
        BigDecimal it = safeDivide(cost, average(inventory, bsBeginningData.get("存货")));
        result.put("inventoryTurnoverDays",
                it != null ? new BigDecimal("365").divide(it, SCALE, ROUNDING_MODE) : null);

        // 流动资产周转率 = 营业收入 / 平均流动资产
        result.put("currentAssetTurnover", safeDivide(revenue,
                average(currentAssets, bsBeginningData.get("流动资产合计"))));

        // 固定资产周转率 = 营业收入 / 固定资产净值
        result.put("fixedAssetTurnover", safeDivide(revenue,
                average(fixedAssets, bsBeginningData.get("固定资产"))));

        // 总资产周转率 = 营业收入 / 平均总资产
        result.put("totalAssetTurnover", safeDivide(revenue,
                average(totalAssets, bsBeginningData.get("资产总计"))));

        return result;
    }

    /**
     * 计算现金流能力指标
     */
    private Map<String, Object> cashFlowAbility(Map<String, BigDecimal> cfData,
                                                 Map<String, BigDecimal> bsData,
                                                 Map<String, BigDecimal> isData) {
        Map<String, Object> result = new LinkedHashMap<>();

        BigDecimal netOperatingCF = cfData.get("经营活动产生的现金流量净额");
        BigDecimal currentLiabilities = bsData.get("流动负债合计");
        BigDecimal totalLiabilities = bsData.get("负债合计");
        BigDecimal netProfit = isData.get("净利润");
        BigDecimal revenue = isData.get("营业收入");
        BigDecimal interestExpense = isData.get("利息支出");

        // 现金流量比率 = 经营活动现金流净额 / 流动负债
        result.put("cashFlowRatio", safeDivide(netOperatingCF, currentLiabilities));

        // 现金负债比率 = 经营活动现金流净额 / 总负债
        result.put("cashDebtRatio", safeDivide(netOperatingCF, totalLiabilities));

        // 经营现金净流量与净利润比率 = 经营现金流净额 / 净利润
        result.put("operatingCashToNetProfit", safeDivide(netOperatingCF, netProfit));

        // 经营现金净流量与营业收入比率 = 经营现金流净额 / 营业收入
        result.put("operatingCashToRevenue", safeDivide(netOperatingCF, revenue));

        // 现金流量利息保障倍数 = 经营现金流净额 / 利息支出
        result.put("cashFlowInterestCoverage", safeDivide(netOperatingCF, interestExpense));

        return result;
    }

    /**
     * 计算成长能力指标（需要历史数据）
     */
    private Map<String, Object> calculateGrowthAbility(Long reportId,
                                                        Map<String, BigDecimal> currentIS,
                                                        Map<String, BigDecimal> currentBS,
                                                        Map<String, BigDecimal> beginningBS,
                                                        Map<String, BigDecimal> currentCF) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 尝试获取上期数据进行对比
        FinancialReportArchive previousArchive = findPreviousArchive(reportId);

        Map<String, BigDecimal> prevIS = previousArchive == null
                ? extractPreviousIncomeStatementData(reportId)
                : extractIncomeStatementData(previousArchive.getId());
        Map<String, BigDecimal> prevBS = previousArchive == null
                ? beginningBS
                : extractBalanceSheetData(previousArchive.getId());
        Map<String, BigDecimal> prevCF = previousArchive == null
                ? Collections.emptyMap()
                : extractCashFlowData(previousArchive.getId());

        // 营业收入增长率
        result.put("revenueGrowthRate", growthRate(currentIS.get("营业收入"), prevIS.get("营业收入")));

        // 净利润增长率
        result.put("netProfitGrowthRate", growthRate(currentIS.get("净利润"), prevIS.get("净利润")));

        // 总资产增长率
        result.put("totalAssetGrowthRate", growthRate(currentBS.get("资产总计"), prevBS.get("资产总计")));

        // 所有者权益增长率
        result.put("equityGrowthRate", growthRate(currentBS.get("所有者权益合计"),
                prevBS.get("所有者权益合计")));

        // 经营现金流增长率
        result.put("operatingCashFlowGrowthRate",
                growthRate(currentCF.get("经营活动产生的现金流量净额"),
                        prevCF.get("经营活动产生的现金流量净额")));
        result.put("prepaymentToCurrentAssets",
                safeDivide(currentBS.get("预付款项"), currentBS.get("流动资产合计")));
        result.put("otherReceivableToCurrentAssets",
                safeDivide(currentBS.get("其他应收款"), currentBS.get("流动资产合计")));

        return result;
    }

    /**
     * 健康度评分计算（五维评分模型）
     *
     * @param reportId 报表ID
     * @return 健康度评分VO
     */
    public HealthScoreVO calculateHealthScore(Long reportId) {
        log.info("计算健康度评分：reportId={}", reportId);
        return calculateHealthScore(reportId, calculateIndicators(reportId));
    }

    private HealthScoreVO calculateHealthScore(Long reportId, Map<String, Object> indicators) {
        HealthScoreVO vo = new HealthScoreVO();
        BalanceSheet scoreBalanceSheet = balanceSheetRepository.findByArchiveId(reportId).orElse(null);
        vo.setReportDate(scoreBalanceSheet != null ? scoreBalanceSheet.getReportDate() : LocalDate.now());

        // 五维权重配置
        BigDecimal solvencyWeight = getDimensionWeight("solvency", "0.30");
        BigDecimal profitabilityWeight = getDimensionWeight("profitability", "0.25");
        BigDecimal cashFlowWeight = getDimensionWeight("cashFlow", "0.20");
        BigDecimal operationWeight = getDimensionWeight("operation", "0.15");
        BigDecimal growthWeight = getDimensionWeight("growth", "0.10");

        vo.setSolvencyWeight(solvencyWeight.multiply(new BigDecimal("100")));
        vo.setProfitabilityWeight(profitabilityWeight.multiply(new BigDecimal("100")));
        vo.setCashFlowWeight(cashFlowWeight.multiply(new BigDecimal("100")));
        vo.setOperationWeight(operationWeight.multiply(new BigDecimal("100")));
        vo.setGrowthWeight(growthWeight.multiply(new BigDecimal("100")));

        // 计算各维度得分（满分100）
        BigDecimal solvencyScore = calculateDimensionScore(indicators, "solvency");
        BigDecimal profitabilityScore = calculateDimensionScore(indicators, "profitability");
        BigDecimal cashFlowScore = calculateDimensionScore(indicators, "cashflow");
        BigDecimal operationScore = calculateDimensionScore(indicators, "operation");
        BigDecimal growthScore = calculateDimensionScore(indicators, "growth");

        vo.setSolvencyScore(solvencyScore);
        vo.setProfitabilityScore(profitabilityScore);
        vo.setCashFlowScore(cashFlowScore);
        vo.setOperationScore(operationScore);
        vo.setGrowthScore(growthScore);

        // 加权计算综合得分
        BigDecimal totalScore = solvencyScore.multiply(solvencyWeight)
                .add(profitabilityScore.multiply(profitabilityWeight))
                .add(cashFlowScore.multiply(cashFlowWeight))
                .add(operationScore.multiply(operationWeight))
                .add(growthScore.multiply(growthWeight));

        FinancialReportArchive scoreArchive = getArchiveById(reportId);
        BigDecimal debtRatio = getBigDecimalValue(indicators, "debtToAssetRatio");
        if ((scoreArchive.getTotalEquity() != null
                && scoreArchive.getTotalEquity().compareTo(BigDecimal.ZERO) <= 0)
                || (debtRatio != null && debtRatio.compareTo(new BigDecimal("100")) >= 0)) {
            totalScore = totalScore.min(new BigDecimal("39"));
        }
        totalScore = totalScore.setScale(0, RoundingMode.HALF_UP);
        vo.setTotalScore(totalScore);

        // 根据得分确定风险等级
        String riskLevel = determineRiskLevel(totalScore);
        String riskLevelDesc = getRiskLevelDescription(riskLevel);
        vo.setRiskLevel(riskLevel);
        vo.setRiskLevelDesc(riskLevelDesc);

        // 生成评价摘要
        vo.setSummary(generateHealthSummary(vo));

        // 保存到数据库
        saveHealthScore(reportId, vo);

        log.info("健康度评分完成：totalScore={}, riskLevel={}", totalScore, riskLevel);
        return vo;
    }

    /**
     * 数据校验（勾稽关系校验）
     *
     * @param reportId 报表ID
     * @return 校验结果列表
     */
    public List<Map<String, Object>> validateFinancialData(Long reportId) {
        log.info("执行数据校验：reportId={}", reportId);

        List<Map<String, Object>> validationResults = new ArrayList<>();
        FinancialReportArchive archive = getArchiveById(reportId);

        // 获取三大报表数据
        Map<String, BigDecimal> bsData = extractBalanceSheetData(reportId);
        Map<String, BigDecimal> isData = extractIncomeStatementData(reportId);
        Map<String, BigDecimal> cfData = extractCashFlowData(reportId);

        // 校验1: 资产负债表平衡：资产总计 ≈ 负债合计 + 所有者权益合计（误差<0.01元）
        validateBalanceSheetBalance(bsData, validationResults);

        // 校验2: 流动资产合计 = 各流动资产项之和
        validateCurrentAssetsSum(bsData, validationResults);

        // 校验3: 流动负债合计 = 各流动负债项之和
        validateCurrentLiabilitiesSum(bsData, validationResults);

        // 校验4: 利润表勾稽关系
        validateIncomeStatementLogic(isData, validationResults);

        // 校验5: 利润总额 >= 净利润
        validateProfitLogic(isData, validationResults);

        // 校验6: 现金流量表勾稽关系
        validateCashFlowLogic(cfData, validationResults);

        // 校验7: （可选）现金流量表期末现金 ≈ 资产负债表货币资金
        validateCashConsistency(cfData, bsData, validationResults);

        log.info("数据校验完成：items={}", validationResults.size());
        return validationResults;
    }

    /**
     * 生成分析报告（基于规则引擎）
     *
     * @param reportId 报表ID
     * @return 分析报告VO
     */
    public AnalysisReportVO generateAnalysisReport(Long reportId) {
        log.info("生成分析报告：reportId={}", reportId);

        FinancialReportArchive archive = getArchiveById(reportId);

        // 计算财务指标和健康评分
        Map<String, Object> indicators = calculateIndicators(reportId);
        HealthScoreVO healthScore = calculateHealthScore(reportId, indicators);

        AnalysisReportVO report = new AnalysisReportVO();
        report.setArchiveId(reportId);
        report.setEnterpriseId(archive.getEnterprise().getId());
        report.setEnterpriseName(archive.getEnterprise().getEnterpriseName());
        report.setReportPeriod(archive.getReportPeriod());
        report.setReportTitle(String.format("%s - %s 财务分析报告",
                archive.getEnterprise().getEnterpriseName(), archive.getReportPeriod()));
        report.setReportType("comprehensive");
        report.setGenerationMethod("rule_based");
        report.setStatus("generated");
        report.setVersion(1);
        report.setCreatedTime(LocalDateTime.now());

        // 生成报告内容
        report.setExecutiveSummary(generateExecutiveSummary(archive, healthScore));
        report.setOverallAssessment(generateOverallAssessment(healthScore, indicators));
        report.setKeyFindings(generateKeyFindings(indicators, healthScore));
        report.setRiskAnalysis(generateRiskAnalysis(indicators, healthScore));
        report.setPositiveFactors(generatePositiveFactors(indicators, healthScore));
        report.setImprovementSuggestions(generateImprovementSuggestions(indicators, healthScore));
        report.setDataQualityNotes(generateDataQualityNotes(archive));

        // 保存报告到数据库
        report = saveAnalysisReport(reportId, report);

        log.info("分析报告生成完成：reportId={}", report.getReportId());
        return report;
    }

    @Override
    public AnalysisReportVO getAnalysisReport(Long reportId) {
        getArchiveById(reportId);
        return analysisReportRepository.findFirstByReportIdAndDeletedOrderByVersionDesc(reportId, 0)
                .map(this::convertToAnalysisReportVO)
                .orElseGet(() -> generateAnalysisReport(reportId));
    }

    @Override
    @Transactional
    public AnalysisReportVO submitAnalysisReport(Long reportId, String username) {
        FinancialReportArchive archive = getArchiveById(reportId);
        getAnalysisReport(reportId);
        FinancialAnalysisReport entity = analysisReportRepository
                .findFirstByReportIdAndDeletedOrderByVersionDesc(reportId, 0)
                .orElseThrow(() -> new BusinessException(404, "分析报告不存在"));
        if ("approved".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(409, "该分析报告已经审批完成");
        }
        if (!"pending_approval".equalsIgnoreCase(entity.getStatus())) {
            entity.setStatus("pending_approval");
            entity.setSubmittedBy(username);
            entity.setSubmittedTime(LocalDateTime.now());
            entity.setApprovedBy(null);
            entity.setApprovedTime(null);
            entity.setUpdatedTime(LocalDateTime.now());
            analysisReportRepository.save(entity);
        }
        archive.setFilingStatus("PENDING_REVIEW");
        archive.setReviewComment(null);
        archive.setReviewedBy(null);
        archive.setReviewedTime(null);
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
        return convertToAnalysisReportVO(entity);
    }

    @Override
    @Transactional
    public AnalysisReportVO completeAnalysisApproval(Long reportId, String username) {
        FinancialReportArchive archive = getArchiveById(reportId);
        FinancialAnalysisReport entity = analysisReportRepository
                .findFirstByReportIdAndDeletedOrderByVersionDesc(reportId, 0)
                .orElseThrow(() -> new BusinessException(404, "分析报告不存在，请先在小程序生成并提交审批"));
        if ("approved".equalsIgnoreCase(entity.getStatus())) {
            return convertToAnalysisReportVO(entity);
        }
        if (!"pending_approval".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(409, "该分析报告尚未提交审批");
        }
        entity.setStatus("approved");
        entity.setApprovedBy(username);
        entity.setApprovedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        analysisReportRepository.save(entity);
        User reviewer = userRepository.findByUsername(username).orElse(null);
        archive.setFilingStatus("APPROVED");
        archive.setReviewedBy(reviewer);
        archive.setReviewedTime(LocalDateTime.now());
        archive.setReviewComment("管理员终审通过");
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
        return convertToAnalysisReportVO(entity);
    }

    @Override
    @Transactional
    public AnalysisReportVO rejectAnalysisApproval(Long reportId, String username, String reason) {
        FinancialReportArchive archive = getArchiveById(reportId);
        FinancialAnalysisReport entity = analysisReportRepository
                .findFirstByReportIdAndDeletedOrderByVersionDesc(reportId, 0)
                .orElseThrow(() -> new BusinessException(404, "分析报告不存在，请先提交审批"));
        if (!"pending_approval".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(409, "该分析报告当前不在待审批状态");
        }
        String rejectionReason = reason == null || reason.trim().isEmpty()
                ? "管理员退回，请重新核对报表数据" : reason.trim();
        entity.setStatus("rejected");
        entity.setApprovedBy(username);
        entity.setApprovedTime(LocalDateTime.now());
        entity.setUpdatedTime(LocalDateTime.now());
        analysisReportRepository.save(entity);

        User reviewer = userRepository.findByUsername(username).orElse(null);
        archive.setFilingStatus("REJECTED");
        archive.setReviewedBy(reviewer);
        archive.setReviewedTime(LocalDateTime.now());
        archive.setReviewComment(rejectionReason);
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
        return convertToAnalysisReportVO(entity);
    }

    /**
     * 历史趋势数据查询
     *
     * @param enterpriseId   企业ID
     * @param indicatorCode  指标编码
     * @param periods        期数
     * @return 趋势数据VO
     */
    public TrendVO getTrendData(Long enterpriseId, String indicatorCode, Integer periods) {
        log.info("查询趋势数据：enterpriseId={}, indicatorCode={}, periods={}",
                enterpriseId, indicatorCode, periods);

        TrendVO trendVO = new TrendVO();
        trendVO.setIndicatorCode(indicatorCode);
        trendVO.setIndicatorName(getIndicatorName(indicatorCode));
        trendVO.setCategory(getIndicatorCategory(indicatorCode));
        trendVO.setUnit(getIndicatorUnit(indicatorCode));

        // 获取企业最近的报表归档
        int limit = periods != null ? periods : 5;
        List<FinancialReportArchive> archives = archiveRepository
                .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(enterpriseId, 0,
                        org.springframework.data.domain.PageRequest.of(0, limit));
        Collections.reverse(archives);

        List<TrendVO.TrendDataItem> dataList = new ArrayList<>();
        BigDecimal previousValue = null;

        for (int i = 0; i < archives.size(); i++) {
            FinancialReportArchive archive = archives.get(i);
            TrendVO.TrendDataItem item = new TrendVO.TrendDataItem();

            item.setReportPeriod(archive.getReportPeriod());
            int reportMonth = archive.getReportMonth() != null
                    ? archive.getReportMonth()
                    : archive.getReportQuarter() != null ? archive.getReportQuarter() * 3 : 12;
            item.setReportDate(YearMonth.of(archive.getReportYear(), reportMonth).atEndOfMonth());

            // 获取该期的指标值
            BigDecimal value = getIndicatorValueForArchive(archive.getId(), indicatorCode);
            item.setValue(value);

            // 计算环比变动率
            if (previousValue != null && value != null && previousValue.compareTo(BigDecimal.ZERO) != 0) {
                BigDecimal changeRate = value.subtract(previousValue)
                        .multiply(new BigDecimal("100"))
                        .divide(previousValue, SCALE, ROUNDING_MODE);
                item.setChangeRate(changeRate);
            }

            // 设置趋势方向
            if (value != null && previousValue != null) {
                int cmp = value.compareTo(previousValue);
                if (cmp > 0) item.setTrendDirection("UP");
                else if (cmp < 0) item.setTrendDirection("DOWN");
                else item.setTrendDirection("STABLE");
            }

            dataList.add(item);
            previousValue = value;
        }

        trendVO.setDataList(dataList);

        log.info("趋势数据查询完成：dataPoints={}", dataList.size());
        return trendVO;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 安全除法运算
     */
    private BigDecimal safeDivide(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return dividend.divide(divisor, SCALE, ROUNDING_MODE);
    }

    private BigDecimal average(BigDecimal first, BigDecimal second) {
        if (first == null || second == null) {
            return first != null ? first : second;
        }
        return first.add(second).divide(new BigDecimal("2"), SCALE, ROUNDING_MODE);
    }

    /**
     * 计算百分比
     */
    private BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (part == null || total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return part.multiply(new BigDecimal("100")).divide(total, SCALE, ROUNDING_MODE);
    }

    /**
     * 计算增长率
     */
    private BigDecimal growthRate(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous).multiply(new BigDecimal("100"))
                .divide(previous.abs(), SCALE, ROUNDING_MODE);
    }

    /**
     * 获取归档记录
     */
    private FinancialReportArchive getArchiveById(Long archiveId) {
        return archiveRepository.findById(archiveId)
                .filter(a -> a.getDeleted() == 0)
                .orElseThrow(() -> new BusinessException(404, "报表归档不存在或已被删除"));
    }

    /**
     * 从资产负债表提取关键数据
     */
    private Map<String, BigDecimal> extractBalanceSheetData(Long reportId) {
        Map<String, BigDecimal> data = new HashMap<>();
        BalanceSheet balanceSheet = balanceSheetRepository.findByArchiveId(reportId).orElse(null);
        List<BalanceSheetItem> items = balanceSheet == null
                ? Collections.emptyList()
                : balanceSheetItemRepository.findByBalanceSheetId(balanceSheet.getId());

        for (BalanceSheetItem item : items) {
            data.put(item.getItemName(), item.getEndingBalance());
            if (item.getItemName().startsWith("所有者权益（或股东权益）合计")) {
                data.put("所有者权益合计", item.getEndingBalance());
            }
        }

        // 如果没有明细数据，尝试从归档记录获取
        if (data.isEmpty()) {
            FinancialReportArchive archive = archiveRepository.findById(reportId).orElse(null);
            if (archive != null) {
                data.put("资产总计", archive.getTotalAssets());
                data.put("负债合计", archive.getTotalLiabilities());
                data.put("所有者权益合计", archive.getTotalEquity());
            }
        }

        return data;
    }

    /**
     * 从利润表提取关键数据
     */
    private Map<String, BigDecimal> extractIncomeStatementData(Long reportId) {
        Map<String, BigDecimal> data = new HashMap<>();
        IncomeStatement incomeStatement = incomeStatementRepository.findByArchiveId(reportId).orElse(null);
        List<IncomeStatementItem> items = incomeStatement == null
                ? Collections.emptyList()
                : incomeStatementItemRepository.findByIncomeStatementId(incomeStatement.getId());

        for (IncomeStatementItem item : items) {
            data.put(item.getItemName(), item.getCurrentPeriodAmount());
            data.put(normalizeIncomeItemName(item.getItemName()), item.getCurrentPeriodAmount());
        }

        return data;
    }

    /**
     * 从现金流量表提取关键数据
     */
    private Map<String, BigDecimal> extractCashFlowData(Long reportId) {
        Map<String, BigDecimal> data = new HashMap<>();
        List<CashFlowStatementItem> items = cashFlowStatementItemRepository
                .findByStatementArchiveId(reportId);

        for (CashFlowStatementItem item : items) {
            BigDecimal amount = currentCashAmount(item);
            data.put(item.getItemName(), amount);
            String name = item.getItemName();
            if (name != null && name.contains("经营活动产生的现金流量净额")) {
                data.put("经营活动产生的现金流量净额", amount);
            } else if (name != null && name.contains("现金及现金等价物净增加额")) {
                data.put("现金净增加额", amount);
            } else if (name != null && name.contains("期初现金及现金等价物余额")) {
                data.put("期初现金余额", amount);
            } else if (name != null && name.contains("期末现金及现金等价物余额")) {
                data.put("期末现金余额", amount);
            }
        }

        return data;
    }

    private BigDecimal currentCashAmount(CashFlowStatementItem item) {
        return item.getCurrentPeriodAmount() != null ? item.getCurrentPeriodAmount() : item.getAmount();
    }

    private Map<String, BigDecimal> extractBalanceSheetBeginningData(Long reportId) {
        Map<String, BigDecimal> data = new HashMap<>();
        BalanceSheet balanceSheet = balanceSheetRepository.findByArchiveId(reportId).orElse(null);
        if (balanceSheet == null) {
            return data;
        }
        for (BalanceSheetItem item :
                balanceSheetItemRepository.findByBalanceSheetId(balanceSheet.getId())) {
            data.put(item.getItemName(), item.getBeginningBalance());
            if (item.getItemName().startsWith("所有者权益（或股东权益）合计")) {
                data.put("所有者权益合计", item.getBeginningBalance());
            }
        }
        return data;
    }

    private Map<String, BigDecimal> extractPreviousIncomeStatementData(Long reportId) {
        Map<String, BigDecimal> data = new HashMap<>();
        IncomeStatement statement = incomeStatementRepository.findByArchiveId(reportId).orElse(null);
        if (statement == null) {
            return data;
        }
        for (IncomeStatementItem item :
                incomeStatementItemRepository.findByIncomeStatementId(statement.getId())) {
            data.put(normalizeIncomeItemName(item.getItemName()), item.getPreviousPeriodAmount());
        }
        return data;
    }

    private String normalizeIncomeItemName(String itemName) {
        String normalized = itemName
                .replaceFirst("^[一二三四五六七八九十]+、", "")
                .replaceFirst("^(减：|加：|其中：)", "")
                .replaceFirst("（.*$", "")
                .trim();
        return "利息费用".equals(normalized) ? "利息支出" : normalized;
    }

    /**
     * 计算单维度得分
     */
    private BigDecimal calculateDimensionScore(Map<String, Object> indicators, String dimension) {
        switch (dimension) {
            case "solvency":
                return averageScores(
                        scoreAtLeast(indicators, "currentRatio", "1.5", "1"),
                        scoreAtLeast(indicators, "quickRatio", "1", "0.7"),
                        scoreAtLeast(indicators, "cashRatio", "0.2", "0.1"),
                        scoreAtMost(indicators, "debtToAssetRatio", "60", "80"));
            case "profitability":
                return averageScores(
                        scoreAtLeast(indicators, "grossProfitMargin", "20", "10"),
                        scoreAtLeast(indicators, "operatingProfitMargin", "5", "0"),
                        scoreAtLeast(indicators, "netProfitMargin", "3", "0"),
                        scoreAtLeast(indicators, "roa", "3", "0"));
            case "cashflow":
                return averageScores(
                        scoreAtLeast(indicators, "operatingCashToRevenue", "0.08", "0"),
                        scoreAtLeast(indicators, "cashFlowRatio", "0.2", "0"));
            case "operation":
                return averageScores(
                        scoreAtLeast(indicators, "accountsReceivableTurnover", "6", "2"),
                        scoreAtLeast(indicators, "currentAssetTurnover", "1.5", "0.8"),
                        scoreAtLeast(indicators, "totalAssetTurnover", "1", "0.5"));
            case "growth":
                return averageScores(
                        scoreAtLeast(indicators, "revenueGrowthRate", "10", "0"),
                        scoreAtMost(indicators, "prepaymentToCurrentAssets", "0.15", "0.3"),
                        scoreAtMost(indicators, "otherReceivableToCurrentAssets", "0.1", "0.2"));
            default:
                throw new BusinessException(422, "未知的健康评分维度：" + dimension);
        }
    }

    private BigDecimal scoreAtLeast(Map<String, Object> indicators, String code,
                                    String normalThreshold, String attentionThreshold) {
        BigDecimal value = getBigDecimalValue(indicators, code);
        if (value == null) {
            return new BigDecimal("9");
        }
        BigDecimal normal = resolveRuleThreshold(code, "AT_LEAST", true, normalThreshold);
        BigDecimal attention = resolveRuleThreshold(code, "AT_LEAST", false, attentionThreshold);
        if (value.compareTo(normal) >= 0) {
            return new BigDecimal("100");
        }
        return value.compareTo(attention) >= 0
                ? new BigDecimal("50") : new BigDecimal("9");
    }

    private BigDecimal scoreAtMost(Map<String, Object> indicators, String code,
                                   String normalThreshold, String attentionThreshold) {
        BigDecimal value = getBigDecimalValue(indicators, code);
        if (value == null) {
            return new BigDecimal("9");
        }
        BigDecimal normal = resolveRuleThreshold(code, "AT_MOST", true, normalThreshold);
        BigDecimal attention = resolveRuleThreshold(code, "AT_MOST", false, attentionThreshold);
        if (value.compareTo(normal) <= 0) {
            return new BigDecimal("100");
        }
        return value.compareTo(attention) <= 0
                ? new BigDecimal("50") : new BigDecimal("9");
    }

    private BigDecimal resolveRuleThreshold(String code, String direction, boolean normal,
                                            String defaultValue) {
        return indicatorRuleConfigRepository.findFirstByIndicatorCodeAndDeleted(code, 0)
                .filter(rule -> Boolean.TRUE.equals(rule.getIsEnabled()))
                .filter(rule -> direction.equals(rule.getThresholdDirection()))
                .map(rule -> normal ? rule.getNormalThresholdValue() : rule.getAttentionThresholdValue())
                .orElse(new BigDecimal(defaultValue));
    }

    private BigDecimal getDimensionWeight(String dimensionCode, String defaultValue) {
        return healthWeightConfigRepository.findByDimensionCode(dimensionCode)
                .filter(config -> config.getWeight() != null)
                .map(config -> new BigDecimal(config.getWeight())
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP))
                .orElse(new BigDecimal(defaultValue));
    }

    private BigDecimal averageScores(BigDecimal... scores) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal score : scores) {
            total = total.add(score);
        }
        return total.divide(new BigDecimal(scores.length), 2, RoundingMode.HALF_UP);
    }

    /**
     * 根据总分确定风险等级
     */
    private String determineRiskLevel(BigDecimal totalScore) {
        double score = totalScore.doubleValue();
        if (score >= 80) return "HEALTHY";
        if (score >= 60) return "NORMAL";
        if (score >= 40) return "ATTENTION";
        return "DANGEROUS";
    }

    /**
     * 获取风险等级描述
     */
    private String getRiskLevelDescription(String riskLevel) {
        switch (riskLevel) {
            case "HEALTHY": return "健康（正常）";
            case "NORMAL": return "正常";
            case "ATTENTION": return "关注";
            case "DANGEROUS": return "高风险";
            case "CRITICAL": return "严重风险（严重）";
            default: return "未知";
        }
    }

    /**
     * 生成健康评价摘要
     */
    private String generateHealthSummary(HealthScoreVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("综合评分为%.2f分，风险等级为【%s】。", vo.getTotalScore(), vo.getRiskLevelDesc()));

        if (vo.getSolvencyScore() != null && vo.getSolvencyScore().doubleValue() < 60) {
            sb.append("偿债能力偏弱，");
        }
        if (vo.getProfitabilityScore() != null && vo.getProfitabilityScore().doubleValue() < 60) {
            sb.append("盈利能力有待提升，");
        }
        if (vo.getCashFlowScore() != null && vo.getCashFlowScore().doubleValue() < 60) {
            sb.append("现金流质量需关注，");
        }

        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '，') {
            sb.deleteCharAt(sb.length() - 1);
            sb.append("。");
        } else {
            sb.append("整体财务状况良好。");
        }

        return sb.toString();
    }

    /**
     * 数据校验辅助方法
     */
    private void addValidationResult(List<Map<String, Object>> results, String checkName,
                                     boolean passed, String detail, String suggestion) {
        Map<String, Object> result = new HashMap<>();
        result.put("checkName", checkName);
        result.put("passed", passed);
        result.put("detail", detail);
        result.put("suggestion", suggestion);
        results.add(result);
    }

    private void validateBalanceSheetBalance(Map<String, BigDecimal> data,
                                             List<Map<String, Object>> results) {
        BigDecimal totalAssets = data.get("资产总计");
        BigDecimal totalLiabilities = data.get("负债合计");
        BigDecimal totalEquity = data.get("所有者权益合计");

        if (totalAssets != null && totalLiabilities != null && totalEquity != null) {
            BigDecimal expected = totalLiabilities.add(totalEquity);
            BigDecimal diff = totalAssets.subtract(expected).abs();
            boolean passed = diff.compareTo(new BigDecimal("0.01")) <= 0;

            addValidationResult(results, "资产负债表平衡校验", passed,
                    String.format("资产总计=%.2f元，负债及权益合计=%.2f元，差额=%.4f元",
                            totalAssets, expected, diff),
                    passed ? "平衡" : "差额较大，请检查录入数据");
        }
    }

    private void validateCurrentAssetsSum(Map<String, BigDecimal> data,
                                          List<Map<String, Object>> results) {
        validateNamedItemsSum(data, results, "流动资产合计校验", "流动资产合计",
                new String[]{"货币资金", "交易性金融资产", "应收票据", "应收账款", "预付款项",
                        "其他应收款", "存货", "持有待售资产", "一年内到期的非流动资产", "其他流动资产"});
    }

    private void validateCurrentLiabilitiesSum(Map<String, BigDecimal> data,
                                               List<Map<String, Object>> results) {
        validateNamedItemsSum(data, results, "流动负债合计校验", "流动负债合计",
                new String[]{"短期借款", "交易性金融负债", "应付票据", "应付账款", "预收款项",
                        "合同负债", "应付职工薪酬", "应交税费", "其他应付款", "持有待售负债",
                        "一年内到期的非流动负债", "其他流动负债"});
    }

    private void validateNamedItemsSum(Map<String, BigDecimal> data,
                                       List<Map<String, Object>> results,
                                       String checkName, String totalName, String[] itemNames) {
        BigDecimal declared = data.get(totalName);
        if (declared == null) {
            addValidationResult(results, checkName, false,
                    "缺少" + totalName + "，无法完成勾稽校验", "请补录汇总项目");
            return;
        }
        BigDecimal calculated = BigDecimal.ZERO;
        for (String itemName : itemNames) {
            BigDecimal value = data.get(itemName);
            if (value != null) {
                calculated = calculated.add(value);
            }
        }
        BigDecimal difference = declared.subtract(calculated).abs();
        boolean passed = difference.compareTo(new BigDecimal("0.01")) <= 0;
        addValidationResult(results, checkName, passed,
                String.format("申报合计=%s元，明细求和=%s元，差额=%s元",
                        declared, calculated, difference),
                passed ? "勾稽一致" : "请检查汇总数或明细项目");
    }

    private void validateIncomeStatementLogic(Map<String, BigDecimal> data,
                                              List<Map<String, Object>> results) {
        BigDecimal revenue = data.get("营业收入");
        BigDecimal cost = data.get("营业成本");
        BigDecimal tax = data.get("税金及附加");
        BigDecimal salesExp = data.get("销售费用");
        BigDecimal adminExp = data.get("管理费用");
        BigDecimal financeExp = data.get("财务费用");
        BigDecimal operatingProfit = data.get("营业利润");

        if (revenue != null && cost != null && operatingProfit != null) {
            BigDecimal calculated = revenue.subtract(cost).subtract(tax != null ? tax : BigDecimal.ZERO)
                    .subtract(salesExp != null ? salesExp : BigDecimal.ZERO)
                    .subtract(adminExp != null ? adminExp : BigDecimal.ZERO)
                    .subtract(financeExp != null ? financeExp : BigDecimal.ZERO);
            BigDecimal diff = operatingProfit.subtract(calculated).abs();
            boolean passed = diff.compareTo(new BigDecimal("0.01")) <= 0;

            addValidationResult(results, "利润表逻辑校验", passed,
                    String.format("营业利润=%s元，按公式计算=%s元", operatingProfit, calculated),
                    passed ? "逻辑正确" : "存在差异，请检查各项费用");
        }
    }

    private void validateProfitLogic(Map<String, BigDecimal> data,
                                     List<Map<String, Object>> results) {
        BigDecimal totalProfit = data.get("利润总额");
        BigDecimal netProfit = data.get("净利润");

        if (totalProfit != null && netProfit != null) {
            boolean passed = totalProfit.compareTo(netProfit) >= 0;
            addValidationResult(results, "利润总额>=净利润校验", passed,
                    String.format("利润总额=%s元，净利润=%s元", totalProfit, netProfit),
                    passed ? "符合逻辑" : "异常：净利润不应大于利润总额");
        }
    }

    private void validateCashFlowLogic(Map<String, BigDecimal> data,
                                       List<Map<String, Object>> results) {
        BigDecimal inflow = data.get("经营活动现金流入小计");
        BigDecimal outflow = data.get("经营活动现金流出小计");
        BigDecimal netOperating = data.get("经营活动产生的现金流量净额");
        if (inflow != null && outflow != null && netOperating != null) {
            BigDecimal calculated = inflow.subtract(outflow);
            BigDecimal difference = netOperating.subtract(calculated).abs();
            boolean passed = difference.compareTo(new BigDecimal("0.01")) <= 0;
            addValidationResult(results, "经营现金流勾稽校验", passed,
                    String.format("申报净额=%s元，流入减流出=%s元，差额=%s元",
                            netOperating, calculated, difference),
                    passed ? "勾稽一致" : "请检查经营活动现金流明细");
        } else {
            addValidationResult(results, "经营现金流勾稽校验", false,
                    "经营活动现金流关键字段不完整", "请补录流入、流出小计及净额");
        }

        BigDecimal beginning = data.get("期初现金余额");
        BigDecimal increase = data.get("现金净增加额");
        BigDecimal ending = data.get("期末现金余额");
        if (beginning != null && increase != null && ending != null) {
            BigDecimal calculated = beginning.add(increase);
            BigDecimal difference = ending.subtract(calculated).abs();
            boolean passed = difference.compareTo(new BigDecimal("0.01")) <= 0;
            addValidationResult(results, "期末现金勾稽校验", passed,
                    String.format("申报期末=%s元，期初加净增加=%s元，差额=%s元",
                            ending, calculated, difference),
                    passed ? "勾稽一致" : "请检查期初、净增加额或期末余额");
        }
    }

    private void validateCashConsistency(Map<String, BigDecimal> cfData,
                                         Map<String, BigDecimal> bsData,
                                         List<Map<String, Object>> results) {
        BigDecimal cashFromCF = cfData.get("期末现金余额");
        BigDecimal cashFromBS = bsData.get("货币资金");

        if (cashFromCF != null && cashFromBS != null) {
            BigDecimal diff = cashFromCF.subtract(cashFromBS).abs();
            boolean passed = diff.compareTo(new BigDecimal("0.01")) <= 0;

            addValidationResult(results, "现金一致性校验", passed,
                    String.format("现金流量表现金=%s元，资产负债表货币资金=%s元", cashFromCF, cashFromBS),
                    passed ? "基本一致" : "存在差异，可能由于调整项目导致");
        }
    }

    /**
     * 分析报告内容生成方法
     */
    private String generateExecutiveSummary(FinancialReportArchive archive, HealthScoreVO healthScore) {
        return String.format("本报告针对%s的%s财务数据进行分析。综合健康评分为%.2f分，风险等级为【%s】。",
                archive.getEnterprise().getEnterpriseName(),
                archive.getReportPeriod(),
                healthScore.getTotalScore(),
                healthScore.getRiskLevelDesc());
    }

    private String generateOverallAssessment(HealthScoreVO healthScore,
                                             Map<String, Object> indicators) {
        StringBuilder sb = new StringBuilder();
        sb.append("一、总体评价\n\n");
        sb.append(String.format("该企业本期财务状况综合评分为%.2f分，处于【%s】水平。\n\n",
                healthScore.getTotalScore(), healthScore.getRiskLevelDesc()));

        sb.append("各维度评分情况：\n");
        sb.append(String.format("- 偿债能力：%.2f分（权重30%%）\n", healthScore.getSolvencyScore()));
        sb.append(String.format("- 盈利能力：%.2f分（权重25%%）\n", healthScore.getProfitabilityScore()));
        sb.append(String.format("- 现金流质量：%.2f分（权重20%%）\n", healthScore.getCashFlowScore()));
        sb.append(String.format("- 运营效率：%.2f分（权重15%%）\n", healthScore.getOperationScore()));
        sb.append(String.format("- 成长性：%.2f分（权重10%%）\n", healthScore.getGrowthScore()));

        return sb.toString();
    }

    private String generateKeyFindings(Map<String, Object> indicators, HealthScoreVO healthScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n二、核心发现\n\n");

        // 关键指标
        BigDecimal debtRatio = getBigDecimalValue(indicators, "debtToAssetRatio");
        if (debtRatio != null) {
            sb.append(String.format("1. 资产负债率为%.2f%%，", debtRatio));
            if (debtRatio.compareTo(new BigDecimal("60")) <= 0) {
                sb.append("处于合理水平。\n");
            } else if (debtRatio.compareTo(new BigDecimal("80")) <= 0) {
                sb.append("偏高，需关注偿债压力。\n");
            } else {
                sb.append("过高，存在较大的财务风险。\n");
            }
        }

        BigDecimal roe = getBigDecimalValue(indicators, "roe");
        if (roe != null) {
            sb.append(String.format("2. 净资产收益率（ROE）为%.2f%%，", roe));
            if (roe.compareTo(new BigDecimal("15")) >= 0) {
                sb.append("盈利能力强。\n");
            } else if (roe.compareTo(new BigDecimal("8")) >= 0) {
                sb.append("处于行业平均水平。\n");
            } else {
                sb.append("偏低，需提升盈利能力。\n");
            }
        }

        return sb.toString();
    }

    private String generateRiskAnalysis(Map<String, Object> indicators, HealthScoreVO healthScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n三、风险分析\n\n");

        // 偿债风险
        BigDecimal currentRatio = getBigDecimalValue(indicators, "currentRatio");
        if (currentRatio != null && currentRatio.compareTo(new BigDecimal("1.5")) < 0) {
            sb.append(String.format("【偿债风险】流动比率为%.2f，低于警戒线1.5，短期偿债能力较弱。\n", currentRatio));
        }

        // 盈利风险
        BigDecimal netProfitMargin = getBigDecimalValue(indicators, "netProfitMargin");
        if (netProfitMargin != null && netProfitMargin.compareTo(new BigDecimal("3")) < 0) {
            sb.append(String.format("【盈利风险】销售净利率仅为%.2f%%，盈利空间有限。\n", netProfitMargin));
        }

        // 现金流风险
        BigDecimal cashFlowRatio = getBigDecimalValue(indicators, "cashFlowRatio");
        if (cashFlowRatio != null && cashFlowRatio.compareTo(BigDecimal.ZERO) < 0) {
            sb.append("【现金流风险】经营现金流为负，现金流紧张。\n");
        }

        if (sb.toString().split("\n").length <= 2) {
            sb.append("当前未发现重大风险因素。\n");
        }

        return sb.toString();
    }

    private String generatePositiveFactors(Map<String, Object> indicators, HealthScoreVO healthScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n四、主要优势\n\n");

        if (healthScore.getSolvencyScore() != null && healthScore.getSolvencyScore().doubleValue() >= 75) {
            sb.append("- 偿债能力较强，财务结构稳健\n");
        }
        if (healthScore.getProfitabilityScore() != null && healthScore.getProfitabilityScore().doubleValue() >= 75) {
            sb.append("- 盈利能力突出，经营效益良好\n");
        }
        if (healthScore.getCashFlowScore() != null && healthScore.getCashFlowScore().doubleValue() >= 75) {
            sb.append("- 现金流质量优秀，造血能力强\n");
        }
        if (healthScore.getOperationScore() != null && healthScore.getOperationScore().doubleValue() >= 75) {
            sb.append("- 运营效率高，资产管理效果好\n");
        }

        if (sb.indexOf("-") < 0) {
            sb.append("- 本期暂未识别出达到良好阈值的维度，建议结合业务情况持续观察\n");
        }

        return sb.toString();
    }

    private String generateImprovementSuggestions(Map<String, Object> indicators, HealthScoreVO healthScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n五、改善建议\n\n");

        if (healthScore.getSolvencyScore() != null && healthScore.getSolvencyScore().doubleValue() < 65) {
            sb.append("1.【优化资本结构】建议适当降低负债比例，提高流动比率至1.5以上。\n");
        }
        if (healthScore.getProfitabilityScore() != null && healthScore.getProfitabilityScore().doubleValue() < 65) {
            sb.append("2.【提升盈利能力】建议加强成本控制，拓展高毛利业务，提升净利率水平。\n");
        }
        if (healthScore.getCashFlowScore() != null && healthScore.getCashFlowScore().doubleValue() < 65) {
            sb.append("3.【改善现金流】建议加快应收账款回收，优化库存管理，改善经营性现金流。\n");
        }
        if (healthScore.getOperationScore() != null && healthScore.getOperationScore().doubleValue() < 65) {
            sb.append("4.【提高运营效率】建议优化资产配置，加快存货和应收账款周转速度。\n");
        }

        if (sb.toString().split("\n").length <= 2) {
            sb.append("当前财务状况良好，建议继续保持并持续优化各项指标。\n");
        }

        return sb.toString();
    }

    private String generateDataQualityNotes(FinancialReportArchive archive) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n六、数据完整性说明\n\n");
        sb.append(String.format("- 数据来源：%s\n",
                archive.getDataSource() != null ? archive.getDataSource() : "手工录入"));
        sb.append(String.format("- 数据质量评分：%s/100\n",
                archive.getDataQualityScore() != null ? archive.getDataQualityScore().toString() : "未评定"));
        sb.append("- 本报告基于企业提供的财务数据自动生成，仅供业务分析参考，不构成审计、授信审批或投资建议。\n");

        return sb.toString();
    }

    /**
     * 获取BigDecimal类型的指标值
     */
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return null;
    }

    /**
     * 保存指标值到数据库
     */
    private void saveIndicatorValues(Long reportId, Map<String, Object> indicators) {
        // 保存新值（使用report关联）
        FinancialReportArchive archive = archiveRepository.findById(reportId).orElse(null);
        if (archive == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : indicators.entrySet()) {
            if (entry.getValue() instanceof BigDecimal) {
                FinancialIndicatorValue value = indicatorValueRepository
                        .findFirstByReportIdAndIndicatorCodeAndDeletedOrderByIdDesc(
                                reportId, entry.getKey(), 0)
                        .orElse(null);
                if (value == null) {
                    value = new FinancialIndicatorValue();
                    value.setReport(archive);
                    value.setEnterprise(archive.getEnterprise());
                    value.setIndicatorCode(entry.getKey());
                    value.setCreatedTime(LocalDateTime.now());
                    value.setDeleted(0);
                }
                value.setValue((BigDecimal) entry.getValue());
                value.setCalculatedTime(LocalDateTime.now());
                value.setUpdatedTime(LocalDateTime.now());
                indicatorValueRepository.save(value);
            }
        }
    }

    /**
     * 保存健康评分到数据库
     */
    private void saveHealthScore(Long reportId, HealthScoreVO vo) {
        FinancialReportArchive archive = archiveRepository.findById(reportId).orElse(null);
        if (archive == null) {
            return;
        }

        FinancialHealthScore score = healthScoreRepository
                .findFirstByReportIdAndDeletedOrderByIdDesc(reportId, 0)
                .orElseGet(FinancialHealthScore::new);
        if (score.getId() == null) {
            score.setReport(archive);
            score.setEnterprise(archive.getEnterprise());
            score.setCreatedTime(LocalDateTime.now());
            score.setDeleted(0);
        }
        score.setReportDate(vo.getReportDate());
        score.setTotalScore(vo.getTotalScore());
        score.setRiskLevel(vo.getRiskLevel());
        score.setSolvencyScore(vo.getSolvencyScore());
        score.setProfitabilityScore(vo.getProfitabilityScore());
        score.setCashFlowScore(vo.getCashFlowScore());
        score.setOperationScore(vo.getOperationScore());
        score.setGrowthScore(vo.getGrowthScore());
        score.setSolvencyWeight(vo.getSolvencyWeight().divide(new BigDecimal("100")));
        score.setProfitabilityWeight(vo.getProfitabilityWeight().divide(new BigDecimal("100")));
        score.setCashFlowWeight(vo.getCashFlowWeight().divide(new BigDecimal("100")));
        score.setOperationWeight(vo.getOperationWeight().divide(new BigDecimal("100")));
        score.setGrowthWeight(vo.getGrowthWeight().divide(new BigDecimal("100")));
        score.setSummary(vo.getSummary());
        score.setUpdatedTime(LocalDateTime.now());
        healthScoreRepository.save(score);

        // 企业列表/工作台展示的是企业最新一期评分快照。只有当前评分对应
        // 企业最新有效报表时才同步，防止重算历史期覆盖最新期结果。
        List<FinancialReportArchive> latestArchives = archiveRepository
                .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(
                        archive.getEnterprise().getId(), 0, PageRequest.of(0, 1));
        if (!latestArchives.isEmpty() && latestArchives.get(0).getId().equals(reportId)) {
            Enterprise enterprise = archive.getEnterprise();
            enterprise.setHealthScore(vo.getTotalScore() == null ? null
                    : vo.getTotalScore().setScale(0, RoundingMode.HALF_UP).intValue());
            enterprise.setRiskLevel(vo.getRiskLevel());
            enterprise.setLastReportDate(vo.getReportDate());
            enterprise.setUpdatedTime(LocalDateTime.now());
            enterpriseRepository.save(enterprise);
        }

        vo.setScoreId(score.getId());
    }

    /**
     * 保存分析报告到数据库
     */
    private AnalysisReportVO saveAnalysisReport(Long reportId, AnalysisReportVO report) {
        FinancialAnalysisReport entity = analysisReportRepository
                .findFirstByReportIdAndDeletedOrderByVersionDesc(reportId, 0)
                .orElseGet(FinancialAnalysisReport::new);
        // 设置报表归档关联
        FinancialReportArchive archive = archiveRepository.findById(reportId).orElse(null);
        if (archive != null) {
            entity.setReport(archive);
            entity.setEnterprise(archive.getEnterprise());
        }
        entity.setReportTitle(report.getReportTitle());
        entity.setReportType(report.getReportType());
        entity.setExecutiveSummary(report.getExecutiveSummary());
        entity.setOverallAssessment(report.getOverallAssessment());
        entity.setKeyFindings(report.getKeyFindings());
        entity.setRiskAnalysis(report.getRiskAnalysis());
        entity.setPositiveFactors(report.getPositiveFactors());
        entity.setImprovementSuggestions(report.getImprovementSuggestions());
        entity.setDataQualityNotes(report.getDataQualityNotes());
        entity.setGenerationMethod(report.getGenerationMethod());
        entity.setStatus(report.getStatus());
        entity.setVersion(report.getVersion());
        entity.setSubmittedBy(report.getSubmittedBy());
        entity.setSubmittedTime(report.getSubmittedTime());
        entity.setApprovedBy(report.getApprovedBy());
        entity.setApprovedTime(report.getApprovedTime());
        if (entity.getId() == null) {
            entity.setCreatedTime(LocalDateTime.now());
        }
        entity.setUpdatedTime(LocalDateTime.now());
        entity.setDeleted(0);

        entity = analysisReportRepository.save(entity);
        report.setReportId(entity.getId());

        return report;
    }

    private AnalysisReportVO convertToAnalysisReportVO(FinancialAnalysisReport entity) {
        AnalysisReportVO report = new AnalysisReportVO();
        report.setReportId(entity.getId());
        report.setArchiveId(entity.getReport() == null ? null : entity.getReport().getId());
        report.setEnterpriseId(entity.getEnterprise() == null ? null : entity.getEnterprise().getId());
        report.setEnterpriseName(entity.getEnterprise() == null ? null : entity.getEnterprise().getEnterpriseName());
        report.setReportPeriod(entity.getReport() == null ? null : entity.getReport().getReportPeriod());
        report.setReportTitle(entity.getReportTitle());
        report.setReportType(entity.getReportType());
        report.setExecutiveSummary(entity.getExecutiveSummary());
        report.setOverallAssessment(entity.getOverallAssessment());
        report.setKeyFindings(entity.getKeyFindings());
        report.setRiskAnalysis(entity.getRiskAnalysis());
        report.setPositiveFactors(entity.getPositiveFactors());
        report.setImprovementSuggestions(entity.getImprovementSuggestions());
        report.setDataQualityNotes(entity.getDataQualityNotes());
        report.setGenerationMethod(entity.getGenerationMethod());
        report.setVersion(entity.getVersion());
        report.setStatus(entity.getStatus());
        report.setCreatedTime(entity.getCreatedTime());
        report.setSubmittedBy(entity.getSubmittedBy());
        report.setSubmittedTime(entity.getSubmittedTime());
        report.setApprovedBy(entity.getApprovedBy());
        report.setApprovedTime(entity.getApprovedTime());
        return report;
    }

    /**
     * 更新归档记录的汇总数据（从资产负债表）
     */
    private void updateArchiveSummaryFromBalanceSheet(FinancialReportArchive archive,
                                                       List<BalanceSheetItem> items) {
        for (BalanceSheetItem item : items) {
            if ("资产总计".equals(item.getItemName())) {
                archive.setTotalAssets(item.getEndingBalance());
            } else if ("负债合计".equals(item.getItemName())) {
                archive.setTotalLiabilities(item.getEndingBalance());
            } else if ("所有者权益合计".equals(item.getItemName())) {
                archive.setTotalEquity(item.getEndingBalance());
            }
        }
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
    }

    /**
     * 更新归档记录的汇总数据（从利润表）
     */
    private void updateArchiveSummaryFromIncomeStatement(FinancialReportArchive archive,
                                                          List<IncomeStatementItem> items) {
        for (IncomeStatementItem item : items) {
            if ("营业收入".equals(item.getItemName())) {
                archive.setRevenue(item.getCurrentPeriodAmount());
            } else if ("净利润".equals(item.getItemName())) {
                archive.setNetProfit(item.getCurrentPeriodAmount());
            }
        }
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
    }

    private void updateArchiveSummaryFromCashFlowStatement(FinancialReportArchive archive,
                                                            List<CashFlowStatementItem> items) {
        for (CashFlowStatementItem item : items) {
            if ("经营活动产生的现金流量净额".equals(item.getItemName())) {
                archive.setOperatingCashFlow(currentCashAmount(item));
                break;
            }
        }
        archive.setUpdatedTime(LocalDateTime.now());
        archiveRepository.save(archive);
    }

    private void persistReviewedOcrData(Long reportId, List<OcrFieldResult> fields) {
        List<BalanceSheetItem> balanceItems = new ArrayList<>();
        List<IncomeStatementItem> incomeItems = new ArrayList<>();
        List<CashFlowStatementItem> cashFlowItems = new ArrayList<>();
        int balanceOrder = 1;
        int incomeOrder = 1;
        int cashFlowOrder = 1;

        for (OcrFieldResult field : fields) {
            BigDecimal value = parseOcrNumber(field.getFieldValue());
            BigDecimal secondaryValue = parseOcrNumber(field.getSecondaryValue());
            BigDecimal tertiaryValue = parseOcrNumber(field.getTertiaryValue());
            if ("BALANCE_SHEET".equals(field.getFieldType())) {
                BalanceSheetItem item = new BalanceSheetItem();
                item.setItemCode("OCR.BS." + balanceOrder);
                item.setItemName(field.getFieldName());
                item.setItemCategory(resolveBalanceCategory(field.getFieldName()));
                item.setEndingBalance(value);
                item.setBeginningBalance(secondaryValue);
                item.setSortOrder(balanceOrder++);
                item.setIsTotalRow(isSummaryItem(field.getFieldName()) ? 1 : 0);
                balanceItems.add(item);
            } else if ("INCOME_STATEMENT".equals(field.getFieldType())) {
                IncomeStatementItem item = new IncomeStatementItem();
                item.setItemCode("OCR.IS." + incomeOrder);
                item.setItemName(field.getFieldName());
                item.setItemCategory("OCR识别");
                item.setCurrentPeriodAmount(value);
                item.setPreviousPeriodAmount(secondaryValue);
                item.setMonthlyAmount(tertiaryValue);
                item.setSortOrder(incomeOrder++);
                item.setIsTotalRow(isSummaryItem(field.getFieldName()) ? 1 : 0);
                incomeItems.add(item);
            } else if ("CASH_FLOW_STATEMENT".equals(field.getFieldType())) {
                CashFlowStatementItem item = new CashFlowStatementItem();
                item.setItemCode("OCR.CF." + cashFlowOrder);
                item.setItemName(field.getFieldName());
                item.setItemType(resolveCashFlowType(field.getFieldName()));
                item.setAmount(value);
                item.setCurrentPeriodAmount(value);
                item.setPreviousPeriodAmount(secondaryValue);
                item.setMonthlyAmount(tertiaryValue);
                item.setRowNumber(cashFlowOrder++);
                item.setIsTotalRow(isSummaryItem(field.getFieldName()));
                item.setConfidenceLevel(field.getConfidenceLevel());
                item.setOcrSourceField(field.getId().toString());
                cashFlowItems.add(item);
            }
        }

        if (!balanceItems.isEmpty()) {
            saveBalanceSheet(reportId, balanceItems);
        }
        if (!incomeItems.isEmpty()) {
            saveIncomeStatement(reportId, incomeItems);
        }
        if (!cashFlowItems.isEmpty()) {
            saveCashFlowStatement(reportId, cashFlowItems);
        }
    }

    private BigDecimal parseOcrNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("，", "")
                .replace("（", "-")
                .replace("）", "")
                .replace("(", "-")
                .replace(")", "");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean isSummaryItem(String itemName) {
        return itemName != null && (itemName.contains("合计") || itemName.contains("总计")
                || itemName.contains("净额") || itemName.contains("净利润")
                || itemName.contains("利润总额") || itemName.contains("净增加额"));
    }

    private String resolveBalanceCategory(String itemName) {
        if (itemName == null) return "OCR识别";
        if (itemName.contains("权益") || itemName.contains("资本") || itemName.contains("利润")) {
            return "所有者权益";
        }
        if (itemName.contains("负债") || itemName.contains("借款") || itemName.contains("应付")
                || itemName.contains("预收")) {
            return "负债";
        }
        return "资产";
    }

    private String resolveCashFlowType(String itemName) {
        if (itemName == null) return "SUMMARY";
        if (itemName.contains("经营")) return "OPERATING";
        if (itemName.contains("投资")) return "INVESTING";
        if (itemName.contains("筹资") || itemName.contains("借款") || itemName.contains("债务")) {
            return "FINANCING";
        }
        return "SUMMARY";
    }

    private LocalDate resolveReportDate(FinancialReportArchive archive) {
        int month = archive.getReportMonth() != null
                ? archive.getReportMonth()
                : archive.getReportQuarter() != null ? archive.getReportQuarter() * 3 : 12;
        return YearMonth.of(archive.getReportYear(), Math.max(1, Math.min(month, 12))).atEndOfMonth();
    }

    /**
     * 查找上一期归档记录
     */
    private FinancialReportArchive findPreviousArchive(Long currentArchiveId) {
        FinancialReportArchive current = archiveRepository.findById(currentArchiveId).orElse(null);
        if (current == null) return null;

        List<FinancialReportArchive> previousList = archiveRepository
                .findByEnterpriseIdAndCreatedTimeBeforeAndDeletedOrderByCreatedTimeDesc(
                        current.getEnterprise().getId(),
                        current.getCreatedTime(),
                        0);

        return previousList.isEmpty() ? null : previousList.get(0);
    }

    /**
     * 获取指标名称
     */
    private String getIndicatorName(String code) {
        Map<String, String> names = new HashMap<>();
        names.put("currentRatio", "流动比率");
        names.put("quickRatio", "速动比率");
        names.put("debtToAssetRatio", "资产负债率");
        names.put("roe", "净资产收益率ROE");
        names.put("roa", "总资产收益率ROA");
        names.put("netProfitMargin", "销售净利率");
        names.put("grossProfitMargin", "销售毛利率");
        names.put("revenueGrowthRate", "营业收入增长率");
        return names.getOrDefault(code, code);
    }

    /**
     * 获取指标分类
     */
    private String getIndicatorCategory(String code) {
        if (code.contains("Ratio") || code.contains("Debt") || code.contains("Coverage"))
            return "偿债能力";
        if (code.contains("Margin") || code.contains("ROE") || code.contains("ROA"))
            return "盈利能力";
        if (code.contains("Turnover") || code.contains("Days"))
            return "运营效率";
        if (code.contains("Cash") || code.contains("Flow"))
            return "现金流能力";
        if (code.contains("Growth"))
            return "成长能力";
        return "其他";
    }

    /**
     * 获取指标单位
     */
    private String getIndicatorUnit(String code) {
        if (code.contains("Ratio") || code.contains("Rate") || code.contains("Margin")
                || code.contains("ROE") || code.contains("ROA") || code.contains("Growth"))
            return "%";
        if (code.contains("Days"))
            return "天";
        if (code.contains("Capital"))
            return "万元";
        return "";
    }

    /**
     * 获取指定归档的指标值
     */
    private BigDecimal getIndicatorValueForArchive(Long archiveId, String indicatorCode) {
        FinancialIndicatorValue value = indicatorValueRepository
                .findFirstByReportIdAndIndicatorCodeAndDeletedOrderByIdDesc(
                        archiveId, indicatorCode, 0)
                .orElse(null);
        return value != null ? value.getValue() : null;
    }

    /**
     * 获取最新分析概览
     */
    @Override
    public Object getLatestAnalysis(Long enterpriseId) {
        log.info("获取企业最新分析：enterpriseId={}", enterpriseId);

        // 获取该企业最新的报表归档
        List<FinancialReportArchive> archives = archiveRepository
                .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(enterpriseId, 0,
                        org.springframework.data.domain.PageRequest.of(0, 1));

        if (archives.isEmpty()) {
            return null;
        }

        Long latestReportId = archives.get(0).getId();

        // 构建分析概览数据
        Map<String, Object> analysis = new LinkedHashMap<>();

        // 健康评分
        try {
            HealthScoreVO healthScore = calculateHealthScore(latestReportId);
            analysis.put("healthScore", healthScore);
        } catch (Exception e) {
            log.warn("计算健康评分失败", e);
        }

        // 关键指标
        try {
            Map<String, Object> indicators = calculateIndicators(latestReportId);
            // 只返回核心指标
            Map<String, Object> coreIndicators = new LinkedHashMap<>();
            coreIndicators.put("debtToAssetRatio", indicators.get("debtToAssetRatio"));
            coreIndicators.put("roe", indicators.get("roe"));
            coreIndicators.put("currentRatio", indicators.get("currentRatio"));
            coreIndicators.put("netProfitMargin", indicators.get("netProfitMargin"));
            coreIndicators.put("operatingCashToNetProfit", indicators.get("operatingCashToNetProfit"));
            analysis.put("coreIndicators", coreIndicators);
        } catch (Exception e) {
            log.warn("计算指标失败", e);
        }

        return analysis;
    }

    /**
     * 获取三大报表数据
     */
    @Override
    public Object getStatements(Long reportId) {
        log.info("获取三大报表：reportId={}", reportId);

        ReportDetailVO detail = getReportDetail(reportId);
        Map<String, Object> statements = new LinkedHashMap<>();
        statements.put("enterpriseName", detail.getEnterpriseName());
        statements.put("reportPeriod", detail.getReportPeriod());
        statements.put("unit", "元");
        statements.put("healthScore", detail.getHealthScore());
        statements.put("riskLevel", detail.getRiskLevel());
        statements.put("balanceSheet", getBalanceSheetData(reportId));
        statements.put("incomeStatement", getIncomeStatementData(reportId));
        statements.put("cashFlowStatement", getCashFlowStatementData(reportId));

        return statements;
    }

    /**
     * 获取资产负债表数据
     */
    @Override
    public Object getBalanceSheetData(Long reportId) {
        log.info("获取资产负债表：reportId={}", reportId);

        BalanceSheet balanceSheet = balanceSheetRepository.findByArchiveId(reportId).orElse(null);
        if (balanceSheet == null) {
            return missingStatement("资产负债表");
        }

        List<BalanceSheetItem> items = balanceSheetItemRepository.findByBalanceSheetId(balanceSheet.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sheetInfo", balanceSheet);
        result.put("items", items);

        return result;
    }

    /**
     * 获取利润表数据
     */
    @Override
    public Object getIncomeStatementData(Long reportId) {
        log.info("获取利润表：reportId={}", reportId);

        IncomeStatement incomeStatement = incomeStatementRepository.findByArchiveId(reportId).orElse(null);
        if (incomeStatement == null) {
            return missingStatement("利润表");
        }

        List<IncomeStatementItem> items = incomeStatementItemRepository
                .findByIncomeStatementId(incomeStatement.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statementInfo", incomeStatement);
        result.put("items", items);

        return result;
    }

    /**
     * 获取现金流量表数据
     */
    @Override
    public Object getCashFlowStatementData(Long reportId) {
        log.info("获取现金流量表：reportId={}", reportId);

        CashFlowStatement cashFlowStatement = cashFlowStatementRepository.findByArchiveId(reportId).orElse(null);
        if (cashFlowStatement == null) {
            return missingStatement("现金流量表");
        }

        List<CashFlowStatementItem> items = cashFlowStatementItemRepository
                .findByStatementId(cashFlowStatement.getId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("statementInfo", cashFlowStatement);
        result.put("items", items);

        return result;
    }

    /**
     * 获取健康评分趋势
     */
    @Override
    public List<Map<String, Object>> getHealthScoreTrend(Long enterpriseId) {
        log.info("获取健康评分趋势：enterpriseId={}", enterpriseId);

        List<FinancialReportArchive> archives = archiveRepository
                .findTopByEnterpriseIdAndDeletedOrderByReportDateDesc(enterpriseId, 0,
                        org.springframework.data.domain.PageRequest.of(0, 10));

        List<Map<String, Object>> trendList = new ArrayList<>();

        for (FinancialReportArchive archive : archives) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("archiveId", archive.getId());
            point.put("reportPeriod", archive.getReportPeriod());

            // 尝试从数据库获取已计算的健康评分
            FinancialHealthScore score = healthScoreRepository
                    .findFirstByReportIdAndDeletedOrderByIdDesc(archive.getId(), 0)
                    .orElse(null);
            if (score != null) {
                point.put("score", score.getTotalScore());
                point.put("riskLevel", score.getRiskLevel());
            } else {
                // 如果没有，尝试计算（简化处理，设为null）
                point.put("score", null);
                point.put("riskLevel", "UNCALCULATED");
            }

            trendList.add(point);
        }

        // 反转使时间从早到晚
        Collections.reverse(trendList);

        return trendList;
    }

    private Map<String, Object> missingStatement(String statementName) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("statementName", statementName);
        data.put("dataStatus", "MISSING");
        data.put("items", Collections.emptyList());
        return data;
    }
}
