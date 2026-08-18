package com.mtravel.platform.agent.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.common.AgentApiResponse;
import com.mtravel.platform.agent.common.AgentError;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.security.AgentServicePrincipal;
import com.mtravel.platform.agent.security.service.AgentServiceTokenService;
import com.mtravel.platform.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Agent 专用 Bearer Token 认证过滤器。 */
@Component
public class AgentServiceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AgentServiceTokenFilter.class);
    private static final String PREFIX = "/agent/v1/";
    private final AgentServiceTokenService tokenService;
    private final ObjectMapper objectMapper;

    public AgentServiceTokenFilter(AgentServiceTokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !(path.equals("/agent/v1") || path.startsWith(PREFIX));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AgentServicePrincipal principal;
        try {
            principal = tokenService.authenticate(resolveBearerToken(request));
        } catch (AgentException exception) {
            SecurityContextHolder.clearContext();
            writeError(request, response, exception);
            return;
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
            log.error(
                    "agent service token authentication failed, requestId={}",
                    AgentRequestContext.requestId(request),
                    exception
            );
            writeError(request, response, AgentException.serviceUnavailable("Agent 服务令牌校验服务暂时不可用"));
            return;
        }

        List<SimpleGrantedAuthority> authorities = principal.scopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, authorities)
        );
        TenantContextHolder.setTenantId(principal.tenantId());
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            AgentException exception
    ) throws IOException {
        String requestId = AgentRequestContext.requestId(request);
        response.setStatus(exception.httpStatus().value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Request-Id", requestId);
        objectMapper.writeValue(response.getWriter(), AgentApiResponse.fail(
                exception.code(),
                exception.getMessage(),
                requestId,
                new AgentError(exception.errorType(), exception.retryable(), exception.details())
        ));
    }
}
