package com.mtravel.platform.agent.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Agent JSON 请求 64KB 载荷限制测试。 */
class AgentPayloadLimitFilterTest {

    @Test
    void oversizedAgentRequestShouldReturnStablePayloadTooLargeError() throws Exception {
        AgentPayloadLimitFilter filter = new AgentPayloadLimitFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/agent/v1/handoffs");
        request.setContentType("application/json");
        request.setContent("x".repeat(65_537).getBytes(StandardCharsets.UTF_8));
        request.setAttribute(AgentRequestContext.ATTRIBUTE_REQUEST_ID, "request-oversized-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();
        FilterChain chain = (ignoredRequest, ignoredResponse) -> invoked.set(true);

        filter.doFilter(request, response, chain);

        assertThat(invoked).isFalse();
        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("PAYLOAD_TOO_LARGE", "request-oversized-001");
    }

    @Test
    void validAgentRequestShouldContinueWithReusableBody() throws Exception {
        AgentPayloadLimitFilter filter = new AgentPayloadLimitFilter(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/agent/v1/products/search");
        request.setContentType("application/json");
        request.setContent("{\"customerId\":13}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean bodyReadable = new AtomicBoolean();
        FilterChain chain = (wrapped, ignoredResponse) -> bodyReadable.set(
                new String(wrapped.getInputStream().readAllBytes(), StandardCharsets.UTF_8).contains("customerId")
        );

        filter.doFilter(request, response, chain);

        assertThat(bodyReadable).isTrue();
    }
}
