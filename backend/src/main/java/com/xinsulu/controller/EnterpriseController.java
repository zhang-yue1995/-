package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.dto.EnterpriseDTO;
import com.xinsulu.dto.PageQueryDTO;
import com.xinsulu.dto.PageResponse;
import com.xinsulu.service.EnterpriseService;
import com.xinsulu.service.FinancialReportService;
import com.xinsulu.vo.EnterpriseVO;
import com.xinsulu.vo.ReportDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 企业管理控制器
 * 提供企业CRUD、报表查询、财务分析等接口
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/enterprises")
@Api(tags = "企业管理")
public class EnterpriseController {

    @Autowired
    private EnterpriseService enterpriseService;

    @Autowired
    private FinancialReportService financialReportService;

    /**
     * 分页查询企业列表
     * 支持企业名称搜索、风险等级筛选
     *
     * @param pageQueryDTO 分页参数
     * @return 企业分页列表
     */
    @GetMapping
    @ApiOperation(value = "分页查询企业列表", notes = "支持关键词搜索，可按名称模糊匹配")
    public ApiResponse<PageResponse<EnterpriseVO>> list(PageQueryDTO pageQueryDTO) {
        log.info("查询企业列表：page={}, keyword={}", pageQueryDTO.getPageNum(), pageQueryDTO.getKeyword());
        PageResponse<EnterpriseVO> result = enterpriseService.getPage(pageQueryDTO);
        return ApiResponse.success(result);
    }

    @GetMapping("/by-credit-code")
    @ApiOperation(value = "按统一社会信用代码查询已归档企业")
    public ApiResponse<EnterpriseVO> findByCreditCode(@RequestParam String creditCode) {
        return ApiResponse.success(enterpriseService.findActiveByCreditCode(creditCode));
    }

    /**
     * 根据ID查询企业详情
     *
     * @param id 企业ID
     * @return 企业详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "查询企业详情", notes = "根据ID获取企业的完整信息")
    public ApiResponse<EnterpriseVO> detail(@PathVariable Long id) {
        log.info("查询企业详情：id={}", id);
        EnterpriseVO vo = enterpriseService.getById(id);
        return ApiResponse.success(vo);
    }

    /**
     * 新增企业
     *
     * @param enterpriseDTO 企业信息
     * @return 创建后的企业详情
     */
    @PostMapping
    @ApiOperation(value = "新增企业", notes = "创建新的企业档案")
    public ApiResponse<EnterpriseVO> create(@Valid @RequestBody EnterpriseDTO enterpriseDTO) {
        log.info("新增企业：name={}", enterpriseDTO.getName());
        EnterpriseVO vo = enterpriseService.create(enterpriseDTO);
        return ApiResponse.success(vo);
    }

    /**
     * 编辑企业信息
     *
     * @param id            企业ID
     * @param enterpriseDTO 企业信息
     * @return 更新后的企业详情
     */
    @PutMapping("/{id}")
    @ApiOperation(value = "编辑企业", notes = "更新企业基本信息")
    public ApiResponse<EnterpriseVO> update(@PathVariable Long id,
                                            @Valid @RequestBody EnterpriseDTO enterpriseDTO) {
        log.info("编辑企业：id={}, name={}", id, enterpriseDTO.getName());
        EnterpriseVO vo = enterpriseService.update(id, enterpriseDTO);
        return ApiResponse.success(vo);
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "删除企业", notes = "逻辑删除企业档案")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        log.info("删除企业：id={}", id);
        enterpriseService.delete(id);
        return ApiResponse.success();
    }

    /**
     * 查询企业的所有报表历史
     *
     * @param id           企业ID
     * @param pageQueryDTO 分页参数
     * @return 报表分页列表
     */
    @GetMapping("/{id}/reports")
    @ApiOperation(value = "查询企业报表历史", notes = "获取指定企业的所有报表归档记录")
    public ApiResponse<PageResponse<ReportDetailVO>> getReports(@PathVariable Long id,
                                                                 PageQueryDTO pageQueryDTO) {
        log.info("查询企业报表：enterpriseId={}", id);
        PageResponse<ReportDetailVO> result = financialReportService.getReportsByEnterprise(id, pageQueryDTO);
        return ApiResponse.success(result);
    }

    /**
     * 企业的财务分析概览
     *
     * @param id 企业ID
     * @return 财务分析数据
     */
    @GetMapping("/{id}/analysis")
    @ApiOperation(value = "企业财务分析", notes = "获取企业最新的财务健康评分和分析报告摘要")
    public ApiResponse<Object> getAnalysis(@PathVariable Long id) {
        log.info("获取企业财务分析：enterpriseId={}", id);
        // 返回最新的健康评分和关键指标
        Object analysis = financialReportService.getLatestAnalysis(id);
        return ApiResponse.success(analysis);
    }

    /**
     * 企业的历史趋势数据
     *
     * @param id             企业ID
     * @param indicatorCode  指标编码
     * @param periods        查询期数
     * @return 趋势数据
     */
    @GetMapping("/{id}/trends")
    @ApiOperation(value = "企业趋势分析", notes = "获取企业财务指标的历史变化趋势")
    public ApiResponse<Object> getTrends(@PathVariable Long id,
                                         @RequestParam(required = false) String indicatorCode,
                                         @RequestParam(required = false, defaultValue = "5") Integer periods) {
        log.info("获取企业趋势：enterpriseId={}, indicator={}", id, indicatorCode);
        Object trendData = financialReportService.getTrendData(id, indicatorCode, periods);
        return ApiResponse.success(trendData);
    }
}
