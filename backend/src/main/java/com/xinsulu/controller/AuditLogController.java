package com.xinsulu.controller;

import com.xinsulu.common.api.ApiResponse;
import com.xinsulu.entity.AuditLog;
import com.xinsulu.repository.AuditLogRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器
 * 提供操作日志的查询功能，支持多条件筛选
 *
 * @author xinsulu-team
 */
@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@Api(tags = "审计日志管理")
public class AuditLogController {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * 审计日志列表
     * 支持按操作类型、操作人、时间范围筛选
     *
     * @param operationType 操作类型（可选）
     * @param operator      操作人（可选）
     * @param startTime     开始时间（可选）
     * @param endTime       结束时间（可选）
     * @param page          页码
     * @param size          每页大小
     * @return 日志分页列表
     */
    @GetMapping
    @ApiOperation(value = "审计日志列表", notes = "分页查询操作日志，支持按类型、操作人、时间范围筛选")
    public ApiResponse<Page<AuditLog>> list(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String operator,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        log.info("查询审计日志：type={}, operator={}, timeRange=[{}, {}]",
                operationType, operator, startTime, endTime);

        // 构建动态查询条件
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 未删除条件
            predicates.add(cb.equal(root.get("deleted"), 0));

            // 操作类型筛选
            if (operationType != null && !operationType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), operationType));
            }

            // 操作人筛选
            if (operator != null && !operator.trim().isEmpty()) {
                predicates.add(cb.like(root.get("username"),
                        "%" + operator + "%"));
            }

            // 时间范围筛选
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), endTime));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 排序：按时间倒序
        Sort sort = Sort.by(Sort.Direction.DESC, "createdTime");
        PageRequest pageRequest = PageRequest.of(page - 1, size, sort);

        // 执行查询
        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageRequest);

        log.info("审计日志查询完成：total={}", logs.getTotalElements());
        return ApiResponse.success(logs);
    }

    /**
     * 日志详情
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "日志详情", notes = "根据ID获取审计日志的完整信息")
    public ApiResponse<AuditLog> detail(@PathVariable Long id) {
        log.info("查询日志详情：id={}", id);
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("审计日志不存在"));
        return ApiResponse.success(log);
    }

    /**
     * 操作类型统计
     * 统计各操作类型的日志数量分布
     *
     * @return 操作类型分布
     */
    @GetMapping("/operation-type-stats")
    @ApiOperation(value = "操作类型统计", notes = "统计各操作类型的日志数量分布")
    public ApiResponse<List<Map<String, Object>>> getOperationTypeStats() {
        log.info("获取操作类型统计");

        // 获取所有不同的操作类型及其数量
        List<Object[]> stats = auditLogRepository.countGroupByOperationType();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] stat : stats) {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("operationType", stat[0]);
            item.put("count", stat[1]);
            result.add(item);
        }

        return ApiResponse.success(result);
    }

    /**
     * 用户活跃度排行
     * 统计各用户的操作次数排名
     *
     * @param limit 返回条数
     * @return 用户活跃度排行
     */
    @GetMapping("/user-activity-ranking")
    @ApiOperation(value = "用户活跃度排行", notes = "统计各用户的操作次数排行榜")
    public ApiResponse<List<Map<String, Object>>> getUserActivityRanking(
            @RequestParam(defaultValue = "10") Integer limit) {
        log.info("获取用户活跃度排行：limit={}", limit);

        List<Object[]> ranking = auditLogRepository.countGroupByOperatorName(limit);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] item : ranking) {
            Map<String, Object> userStat = new java.util.LinkedHashMap<>();
            userStat.put("operatorName", item[0]);
            userStat.put("operationCount", item[1]);
            result.add(userStat);
        }

        return ApiResponse.success(result);
    }

    /**
     * 导出审计日志
     * 以CSV格式导出符合条件的日志记录
     *
     * @param operationType 操作类型
     * @param startTime     开始时间
     * @param endTime       结束时间
     * @return CSV格式日志文件
     */
    @GetMapping("/export")
    @ApiOperation(value = "导出审计日志", notes = "以CSV格式导出审计日志记录")
    public org.springframework.http.ResponseEntity<String> exportLogs(
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {

        log.info("导出审计日志");

        // 查询符合条件的日志
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("deleted"), 0));

            if (operationType != null && !operationType.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("action"), operationType));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdTime"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdTime"), endTime));
            }

            return cb.and(predicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };

        List<AuditLog> logs = auditLogRepository.findAll(spec,
                Sort.by(Sort.Direction.DESC, "createdTime"));

        // 构建CSV内容
        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作类型,操作模块,操作描述,操作人,操作时间,IP地址\n");

        for (AuditLog log : logs) {
            csv.append(log.getId()).append(",");
            csv.append(log.getAction()).append(",");
            csv.append(log.getModule()).append(",");
            csv.append(csvEscape(log.getDescription())).append(",");
            csv.append(log.getUsername()).append(",");
            csv.append(log.getCreatedTime() != null ?
                    log.getCreatedTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "").append(",");
            csv.append(log.getIpAddress()).append("\n");
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.parseMediaType("text/csv;charset=UTF-8"));
        headers.setContentDispositionFormData("attachment",
                "audit_logs_" + System.currentTimeMillis() + ".csv");

        return org.springframework.http.ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString());
    }

    /**
     * CSV特殊字符转义
     */
    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
