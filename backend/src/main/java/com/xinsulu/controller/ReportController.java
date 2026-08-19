package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.common.exception.BusinessException;
import com.xinsulu.config.ApiAuthInterceptor;
import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.dto.ReportArchiveDTO;
import com.xinsulu.dto.ReportIntakeDTO;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.ReportDetailVO;
import com.xinsulu.vo.AnalysisReportVO;
import com.xinsulu.vo.UserVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 报表管理控制器
 * 提供报表建档、查询、复核、删除等功能
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@Api(tags = "报表管理")
public class ReportController {

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 报表列表查询
     * 支持按企业ID、报告期、状态筛选
     *
     * @param enterpriseId 企业ID（可选）
     * @param period       报告期（可选）
     * @param status       状态（可选）
     * @param pageQueryDTO 分页参数
     * @return 报表分页列表
     */
    @GetMapping
    @ApiOperation(value = "报表列表", notes = "支持按企业、报告期、状态筛选")
    public ApiResponse<PageResponse<ReportDetailVO>> list(
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String status,
            PageQueryDTO pageQueryDTO) {
        log.info("查询报表列表：enterpriseId={}, period={}, status={}", enterpriseId, period, status);

        PageResponse<ReportDetailVO> result =
                financialReportService.getReports(enterpriseId, period, status, pageQueryDTO);
        return ApiResponse.success(result);
    }

    /**
     * 报表详情
     *
     * @param id 归档ID
     * @return 报表详情（包含三大表摘要）
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "报表详情", notes = "获取报表的完整信息和三大表摘要")
    public ApiResponse<ReportDetailVO> detail(@PathVariable Long id) {
        log.info("查询报表详情：archiveId={}", id);
        ReportDetailVO vo = financialReportService.getReportDetail(id);
        return ApiResponse.success(vo);
    }

    /**
     * 报表建档
     * 创建新的报表归档记录
     *
     * @param dto 报表归档信息
     * @return 归档ID
     */
    @PostMapping("/archive")
    @ApiOperation(value = "报表建档", notes = "创建新的报表归档记录，关联企业和报告期")
    public ApiResponse<Long> archive(@Valid @RequestBody ReportArchiveDTO dto) {
        log.info("报表建档：enterpriseId={}, period={}", dto.getEnterpriseId(), dto.getReportPeriod());
        Long archiveId = financialReportService.createArchive(dto);
        return ApiResponse.success(archiveId);
    }

    /**
     * 提交复核结果
     * 批量更新OCR字段的复核状态和修正值
     *
     * @param id      报表ID
     * @param reviews 复核结果列表
     * @return 操作结果
     */
    @PutMapping("/{id}/review")
    @ApiOperation(value = "提交复核结果", notes = "批量提交OCR字段的人工复核结果")
    public ApiResponse<Void> review(@PathVariable Long id,
                                     @RequestBody java.util.List<com.xinsulu.dto.OcrFieldReviewDTO> reviews) {
        log.info("提交复核：reportId={}, fields={}", id, reviews.size());
        financialReportService.reviewOcrFields(id, reviews);
        return ApiResponse.success();
    }

    /**
     * 删除报表（软删除）
     *
     * @param id 归档ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除报表", notes = "软删除报表及其关联数据")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除报表：archiveId={}", id);
        financialReportService.deleteArchive(id);
        return ApiResponse.success();
    }

    /**
     * 获取三大报表数据
     *
     * @param id 归档ID
     * @return 三大报表完整数据
     */
    @GetMapping("/{id}/statements")
    @ApiOperation(value = "获取三大报表", notes = "获取资产负债表、利润表、现金流量表的完整明细数据")
    public ApiResponse<Object> getStatements(@PathVariable Long id) {
        log.info("获取三大报表：archiveId={}", id);
        Object statements = financialReportService.getStatements(id);
        return ApiResponse.success(statements);
    }

    @PostMapping("/intake")
    @ApiOperation(value = "管理端一体化采集", notes = "OCR完成后将企业资料和报表归档在同一事务内保存")
    public ApiResponse<Long> intake(@Valid @RequestBody ReportIntakeDTO dto) {
        return ApiResponse.success(financialReportService.createIntake(dto));
    }

    @GetMapping("/{id}/validations")
    @ApiOperation(value = "数据校验结果", notes = "执行三大报表勾稽关系校验并返回逐项结果")
    public ApiResponse<java.util.List<java.util.Map<String, Object>>> getValidations(@PathVariable Long id) {
        return ApiResponse.success(financialReportService.validateFinancialData(id));
    }

    @PutMapping("/{id}/complete-approval")
    @ApiOperation(value = "完成报告审批", notes = "管理端对整份智能分析报告执行审批完成")
    public ApiResponse<AnalysisReportVO> completeApproval(
            @PathVariable Long id,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new BusinessException(403, "仅管理员可以完成报告审批");
        }
        return ApiResponse.success(financialReportService.completeAnalysisApproval(
                id, currentUser.getUsername()));
    }

    @PutMapping("/{id}/reject-approval")
    @ApiOperation(value = "驳回报告审批", notes = "管理员驳回整份报表，提交人可在小程序修订后重新提交")
    public ApiResponse<AnalysisReportVO> rejectApproval(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @RequestAttribute(ApiAuthInterceptor.CURRENT_USER_ATTRIBUTE) UserVO currentUser) {
        if (!"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            throw new BusinessException(403, "仅管理员可以驳回报告审批");
        }
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.success(financialReportService.rejectAnalysisApproval(
                id, currentUser.getUsername(), reason));
    }
}
