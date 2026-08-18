package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 将 Agent POST JSON 请求严格限制在 64KB，并保留可供 Controller 重读的请求体。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AgentPayloadLimitFilter extends OncePerRequestFilter {

    static final int MAX_PAYLOAD_BYTES = 64 * 1024;
    private static final String PREFIX = "/agent/v1/";
    private final ObjectMapper objectMapper;

    public AgentPayloadLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !"POST".equalsIgnoreCase(request.getMethod())
                || path == null
                || !(path.equals("/agent/v1") || path.startsWith(PREFIX));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_PAYLOAD_BYTES) {
            writePayloadTooLarge(request, response);
            return;
        }
        byte[] body = request.getInputStream().readNBytes(MAX_PAYLOAD_BYTES + 1);
        if (body.length > MAX_PAYLOAD_BYTES) {
            writePayloadTooLarge(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyRequest(request, body), response);
    }

    private void writePayloadTooLarge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestId = AgentRequestContext.requestId(request);
        response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("X-Request-Id", requestId);
        objectMapper.writeValue(response.getWriter(), AgentApiResponse.fail(
                41301,
                "请求体不能超过 64KB",
                requestId,
                new AgentError("PAYLOAD_TOO_LARGE", false, Map.of())
        ));
    }

    /** 缓存小型 Agent JSON 请求体，避免载荷检查后 Controller 无法再读。 */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() { return input.available() == 0; }

                @Override
                public boolean isReady() { return true; }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Spring MVC 在当前同步接口中不使用异步读监听。
                }

                @Override
                public int read() { return input.read(); }

                @Override
                public int read(byte[] bytes, int offset, int length) {
                    return input.read(bytes, offset, length);
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() { return body.length; }

        @Override
        public long getContentLengthLong() { return body.length; }
    }
}
