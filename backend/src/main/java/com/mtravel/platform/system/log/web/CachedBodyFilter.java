package com.mtravel.platform.system.log.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachedBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldWrap(request)) {
            filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldWrap(HttpServletRequest request) {
        String method = request.getMethod();
        String contentType = request.getContentType();
        return "POST".equalsIgnoreCase(method)
                && contentType != null
                && contentType.toLowerCase().contains("application/json");
    }
}
