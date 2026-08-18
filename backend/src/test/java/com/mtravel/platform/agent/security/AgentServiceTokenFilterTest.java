package com.mtravel.platform.agent.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtravel.platform.agent.common.AgentException;
import com.mtravel.platform.agent.common.AgentRequestContext;
import com.mtravel.platform.agent.security.filter.AgentServiceTokenFilter;
import com.mtravel.platform.agent.security.service.AgentServiceTokenService;
import jakarta.servlet.FilterChain;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agent 令牌依赖故障时的稳定错误协议测试。 */
class AgentServiceTokenFilterTest {

    @Test
    void tokenStoreFailureShouldReturnRetryableServiceUnavailable() throws Exception {
        AgentServiceTokenService tokenService = mock(AgentServiceTokenService.class);
        when(tokenService.authenticate("agent-token-value")).thenThrow(new IllegalStateException("db down"));
        AgentServiceTokenFilter filter = new AgentServiceTokenFilter(tokenService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agent/v1/products/32");
        request.addHeader("Authorization", "Bearer agent-token-value");
        request.setAttribute(AgentRequestContext.ATTRIBUTE_REQUEST_ID, "request-token-store-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SERVICE_UNAVAILABLE", "\"retryable\":true");
    }

    @Test
    void downstreamAgentFailureShouldRemainForControllerExceptionHandling() throws Exception {
        AgentServiceTokenService tokenService = mock(AgentServiceTokenService.class);
        when(tokenService.authenticate("agent-token-value"))
                .thenReturn(new AgentServicePrincipal(1L, 2L, "test-token", Set.of("products:read")));
        AgentServiceTokenFilter filter = new AgentServiceTokenFilter(tokenService, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agent/v1/products/32");
        request.addHeader("Authorization", "Bearer agent-token-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AgentException downstreamFailure = AgentException.validation("controller validation", Map.of());
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw downstreamFailure;
        };

        assertThatThrownBy(() -> filter.doFilter(request, response, chain)).isSameAs(downstreamFailure);
        assertThat(response.getContentAsString()).isEmpty();
    }
}
