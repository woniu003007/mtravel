package com.mtravel.platform.agent.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 为 Agent 接口生成或透传安全的 X-Request-Id。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AgentRequestIdFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/agent/v1/";
    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{8,128}$");

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
        String supplied = request.getHeader("X-Request-Id");
        String requestId = supplied != null && REQUEST_ID_PATTERN.matcher(supplied).matches()
                ? supplied
                : AgentRequestContext.newRequestId();
        request.setAttribute(AgentRequestContext.ATTRIBUTE_REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);
        filterChain.doFilter(request, response);
    }
}
