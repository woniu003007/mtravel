package com.mtravel.platform.system.log.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.system.log.dto.OperationLogRecordCommand;
import com.mtravel.platform.system.log.service.OperationLogService;
import com.mtravel.platform.tenant.TenantContextHolder;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.WebUtils;

@Component
public class OperationLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".startTime";
    private static final String ERROR_ATTRIBUTE = OperationLogInterceptor.class.getName() + ".error";

    private final OperationLogService operationLogService;
    private final TenantProperties tenantProperties;
    private final ObjectMapper objectMapper;

    public OperationLogInterceptor(
            OperationLogService operationLogService,
            TenantProperties tenantProperties,
            ObjectMapper objectMapper
    ) {
        this.operationLogService = operationLogService;
        this.tenantProperties = tenantProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (!(handler instanceof HandlerMethod handlerMethod) || shouldSkip(request)) {
            return;
        }
        OperationLog annotation = handlerMethod.getMethodAnnotation(OperationLog.class);
        long start = request.getAttribute(START_TIME_ATTRIBUTE) instanceof Long value ? value : System.currentTimeMillis();
        long duration = Math.max(0, System.currentTimeMillis() - start);
        Throwable error = ex == null ? (Throwable) request.getAttribute(ERROR_ATTRIBUTE) : ex;
        AuthenticatedUser user = currentUser(request);

        operationLogService.record(new OperationLogRecordCommand(
                TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId()),
                user == null ? null : user.id(),
                user == null ? "anonymous" : user.username(),
                annotation == null ? inferModule(request.getRequestURI()) : annotation.module(),
                annotation == null ? inferOperationType(request.getMethod(), request.getRequestURI()) : annotation.type(),
                request.getRequestURI(),
                request.getMethod(),
                requestParams(request),
                clientIp(request),
                request.getHeader("User-Agent"),
                response.getStatus() < 400 && error == null,
                duration,
                error == null ? null : error.getMessage()
        ));
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }

    private AuthenticatedUser currentUser(HttpServletRequest request) {
        Object principal = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication() == null ? null
                : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    private String inferModule(String path) {
        if (path.startsWith("/customer")) {
            return "客户管理";
        }
        if (path.startsWith("/auth")) {
            return "系统登录";
        }
        return "系统";
    }

    private String inferOperationType(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) {
            return "查询";
        }
        if (path.endsWith("/create")) {
            return "新增";
        }
        if (path.endsWith("/update")) {
            return "修改";
        }
        if (path.endsWith("/delete")) {
            return "删除";
        }
        if (path.endsWith("/status")) {
            return "启用";
        }
        if (path.contains("/login")) {
            return "登录";
        }
        if (path.contains("/logout")) {
            return "退出";
        }
        return "其他";
    }

    private String requestParams(HttpServletRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> params.put(key, values.length == 1 ? values[0] : values));
        CachedBodyHttpServletRequest cachedRequest = WebUtils.getNativeRequest(request, CachedBodyHttpServletRequest.class);
        if (cachedRequest != null && !cachedRequest.bodyText().isBlank()) {
            params.put("body", parseBody(cachedRequest.bodyText()));
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception ignored) {
            return params.toString();
        }
    }

    private Object parseBody(String bodyText) {
        try {
            return objectMapper.readValue(bodyText, Object.class);
        } catch (Exception ignored) {
            return bodyText;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
