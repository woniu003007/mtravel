package com.mtravel.platform.agent.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

/** Agent 参数解析错误必须返回稳定协议，不能变成未分类 500。 */
class AgentExceptionHandlerTest {

    @Test
    void malformedJsonShouldReturnInvalidArgument() {
        AgentExceptionHandler handler = new AgentExceptionHandler();
        MockHttpServletRequest request = request();
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "malformed json", new MockHttpInputMessage(new byte[0])
        );

        var response = handler.handleInvalidArgument(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().type()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().requestId()).isEqualTo("request-invalid-001");
    }

    @Test
    void missingQueryParameterShouldReturnInvalidArgument() {
        AgentExceptionHandler handler = new AgentExceptionHandler();

        var response = handler.handleInvalidArgument(
                new MissingServletRequestParameterException("customerId", "long"), request()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().error().details()).containsKey("request");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/agent/v1/quote-requests");
        request.setAttribute(AgentRequestContext.ATTRIBUTE_REQUEST_ID, "request-invalid-001");
        return request;
    }
}
