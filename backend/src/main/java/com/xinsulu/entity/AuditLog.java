package com.xinsulu.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 审计日志实体类
 * 记录系统关键操作日志，用于审计追踪
 *
 * @author xinsulu-team
 */
@Data
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "module", nullable = false, length = 50)
    private String module;            // 模块：auth/enterprise/report/ocr/analysis等

    @Column(name = "action", nullable = false, length = 50)
    private String action;            // 操作：create/update/delete/login/export等

    @Column(name = "target_type", length = 50)
    private String targetType;        // 操作对象类型

    @Column(name = "target_id")
    private Long targetId;            // 操作对象ID

    @Column(name = "description", length = 500)
    private String description;       // 操作描述

    @Column(name = "request_method", length = 10)
    private String RequestMethod;     // HTTP方法：GET/POST/PUT/DELETE

    @Column(name = "request_url", length = 500)
    private String requestUrl;        // 请求URL

    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;     // 请求参数

    @Column(name = "response_status")
    private Integer responseStatus;   // 响应状态码

    @Column(name = "ip_address", length = 50)
    private String ipAddress;         // 客户端IP

    @Column(name = "user_agent", length = 500)
    private String userAgent;         // 客户端信息

    @Column(name = "execution_time") // 执行耗时（毫秒）
    private Long executionTime;

    @Column(name = "is_success")
    private Boolean isSuccess;        // 是否成功

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;      // 错误信息

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(nullable = false)
    private Integer deleted;
}
