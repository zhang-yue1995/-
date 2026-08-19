package com.xinsulu.config;

import com.xinsulu.entity.AuditLog;
import com.xinsulu.repository.AuditLogRepository;
import com.xinsulu.service.AuthService;
import com.xinsulu.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = "xinsulu.audit.start-time";

    private final AuditLogRepository auditLogRepository;
    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception exception) {
        if (!shouldAudit(request)) {
            return;
        }
        try {
            AuditLog log = new AuditLog();
            UserVO user = resolveUser(request);
            log.setUserId(user != null ? user.getId() : null);
            log.setUsername(user != null ? user.getUsername() : "anonymous");
            log.setModule(resolveModule(request.getRequestURI()));
            log.setAction(resolveAction(request.getMethod(), request.getRequestURI()));
            log.setTargetType(log.getModule());
            log.setDescription(request.getMethod() + " " + request.getRequestURI());
            log.setRequestMethod(request.getMethod());
            log.setRequestUrl(request.getRequestURI());
            log.setResponseStatus(response.getStatus());
            log.setIpAddress(resolveClientIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
            log.setExecutionTime(startTime instanceof Long
                    ? System.currentTimeMillis() - (Long) startTime : null);
            log.setIsSuccess(exception == null && response.getStatus() < 400);
            log.setErrorMessage(exception != null ? exception.getMessage() : null);
            log.setCreatedTime(LocalDateTime.now());
            log.setDeleted(0);
            auditLogRepository.save(log);
        } catch (Exception ignored) {
            // 审计记录失败不能影响主业务响应。
        }
    }

    private boolean shouldAudit(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/audit-logs") || "/api/health".equals(uri)) {
            return false;
        }
        String method = request.getMethod();
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method)
                || uri.endsWith("/export");
    }

    private UserVO resolveUser(HttpServletRequest request) {
        try {
            return authService.getCurrentUser(request.getHeader("Authorization"));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String resolveModule(String uri) {
        String path = uri.startsWith("/api/") ? uri.substring(5) : uri;
        int separator = path.indexOf('/');
        return (separator >= 0 ? path.substring(0, separator) : path)
                .replace("-", "_");
    }

    private String resolveAction(String method, String uri) {
        if (uri.endsWith("/export")) return "EXPORT";
        if ("POST".equalsIgnoreCase(method)) return "CREATE";
        if ("PUT".equalsIgnoreCase(method)) return "UPDATE";
        if ("DELETE".equalsIgnoreCase(method)) return "DELETE";
        return method.toUpperCase();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.trim().isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
