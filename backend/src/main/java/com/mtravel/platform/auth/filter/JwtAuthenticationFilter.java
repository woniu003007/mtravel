package com.mtravel.platform.auth.filter;

import com.mtravel.platform.auth.config.SecurityProperties;
import com.mtravel.platform.auth.dto.AuthenticatedUser;
import com.mtravel.platform.auth.service.AuthSessionService;
import com.mtravel.platform.auth.service.JwtService;
import com.mtravel.platform.auth.service.TokenBlacklistService;
import com.mtravel.platform.system.config.service.AuthConfigService;
import com.mtravel.platform.system.log.dto.OperationLogRecordCommand;
import com.mtravel.platform.system.log.service.OperationLogService;
import com.mtravel.platform.tenant.TenantContextHolder;
import com.mtravel.platform.tenant.TenantProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 请求认证过滤器。
 *
 * <p>每个请求进入业务 Controller 前，先解析 Bearer token、校验黑名单、刷新 Redis 在线会话，
 * 再把用户和租户上下文写入 Spring Security 与 TenantContextHolder。</p>
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final SecurityProperties securityProperties;
    private final TenantProperties tenantProperties;
    private final OperationLogService operationLogService;
    private final AuthSessionService authSessionService;
    private final AuthConfigService authConfigService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            TokenBlacklistService tokenBlacklistService,
            SecurityProperties securityProperties,
            TenantProperties tenantProperties,
            OperationLogService operationLogService,
            AuthSessionService authSessionService,
            AuthConfigService authConfigService
    ) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.securityProperties = securityProperties;
        this.tenantProperties = tenantProperties;
        this.operationLogService = operationLogService;
        this.authSessionService = authSessionService;
        this.authConfigService = authConfigService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveBearerToken(request);
            // 已退出登录的 token 会进入黑名单，即使 JWT 自身未过期也不能继续访问接口。
            if (token != null && tokenBlacklistService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
                TenantContextHolder.setTenantId(readTenantHeader(request));
                writeUnauthorized(request, response);
                return;
            }
            if (token != null) {
                try {
                    AuthenticatedUser user = jwtService.parse(token);
                    if (securityProperties.isSessionTimeoutEnabled()) {
                        Duration idleTimeout = authConfigService.getIdleTimeout(user.tenantId());
                        // Redis 会话是无操作退出和同账号单点在线的最终判断依据。当前可通过配置关闭，避免开发阶段频繁强制退出。
                        if (!authSessionService.validateAndRefresh(user, user.sessionId(), idleTimeout)) {
                            SecurityContextHolder.clearContext();
                            TenantContextHolder.setTenantId(user.tenantId());
                            writeUnauthorized(request, response);
                            return;
                        }
                    }
                    List<SimpleGrantedAuthority> authorities = user.roles().stream()
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                            .toList();
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, token, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContextHolder.setTenantId(user.tenantId());
                } catch (JwtException | IllegalArgumentException ex) {
                    SecurityContextHolder.clearContext();
                    TenantContextHolder.setTenantId(readTenantHeader(request));
                    // 公开接口允许无 token 或无效 token 继续访问，其他接口统一返回未登录。
                    if (isPublicRequest(request)) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeUnauthorized(request, response);
                    return;
                }
            } else {
                TenantContextHolder.setTenantId(readTenantHeader(request));
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * 读取租户请求头。
     *
     * <p>首版后台先使用默认租户；保留请求头解析是为了后续多租户入口扩展。</p>
     */
    private Long readTenantHeader(HttpServletRequest request) {
        String value = request.getHeader("X-Tenant-Id");
        if (value == null || value.isBlank()) {
            return tenantProperties.getDefaultTenantId();
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return tenantProperties.getDefaultTenantId();
        }
    }

    /**
     * 从 Authorization 请求头中解析 Bearer token。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring(7);
    }

    /**
     * 判断是否为无需登录即可访问的系统接口。
     */
    private boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/auth/login")
                || path.equals("/auth/refresh")
                || path.equals("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html");
    }

    /**
     * 写入统一的未登录响应，并记录一次登录失效日志。
     */
    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        operationLogService.record(new OperationLogRecordCommand(
                TenantContextHolder.getTenantId(tenantProperties.getDefaultTenantId()),
                null,
                "anonymous",
                "系统登录",
                "其他",
                request.getRequestURI(),
                normalizeMethod(request.getMethod()),
                "",
                clientIp(request),
                request.getHeader("User-Agent"),
                false,
                0L,
                "登录状态已失效"
        ));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"data\":null,\"error\":\"登录状态已失效\",\"message\":\"登录状态已失效\"}");
    }

    /**
     * 前端约定首版只使用 GET 和 POST，日志里也只保留这两类操作。
     */
    private String normalizeMethod(String method) {
        return "POST".equalsIgnoreCase(method) ? "POST" : "GET";
    }

    /**
     * 获取客户端 IP。
     *
     * <p>优先读取反向代理传入的 IP；如果没有代理头，则使用 Servlet 容器拿到的远端地址。</p>
     */
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
